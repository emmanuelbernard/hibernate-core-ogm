/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;

/**
 * Abstract walker for walkers which begin at an entity (criteria
 * queries and entity loaders).
 *
 * @author Gavin King
 */
public abstract class AbstractEntityJoinWalker extends JoinWalker {

	private final OuterJoinLoadable persister;
	private final String alias;

	public AbstractEntityJoinWalker(
			OuterJoinLoadable persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		this( persister, factory, loadQueryInfluencers, null );
	}

	public AbstractEntityJoinWalker(
			OuterJoinLoadable persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers,
			String alias) {
		super( factory, loadQueryInfluencers );
		this.persister = persister;
		this.alias = ( alias == null ) ? generateRootAlias( persister.getEntityName() ) : alias;
	}

	protected final void initAll(
			final String whereString,
			final String orderByString,
			final LockOptions lockOptions) throws MappingException {
		initAll( whereString, orderByString, lockOptions, AssociationInitCallback.NO_CALLBACK );
	}

	protected final void initAll(
			final String whereString,
			final String orderByString,
			final LockOptions lockOptions,
			final AssociationInitCallback callback) throws MappingException {
		walkEntityTree( persister, getAlias() );
		List allAssociations = new ArrayList();
		allAssociations.addAll( associations );
		allAssociations.add( OuterJoinableAssociation.createRoot( persister.getEntityType(), alias, getFactory() ) );
		initPersisters( allAssociations, lockOptions, callback );
		initStatementString( whereString, orderByString, lockOptions );
	}

	protected final void initProjection(
			final String projectionString,
			final String whereString,
			final String orderByString,
			final String groupByString,
			final LockOptions lockOptions) throws MappingException {
		walkEntityTree( persister, getAlias() );
		persisters = new Loadable[0];
		initStatementString(projectionString, whereString, orderByString, groupByString, lockOptions);
	}

	private void initStatementString(
			final String condition,
			final String orderBy,
			final LockOptions lockOptions) throws MappingException {
		initStatementString(null, condition, orderBy, "", lockOptions);
	}

	private void initStatementString(
			final String projection,
			final String condition,
			final String orderBy,
			final String groupBy,
			final LockOptions lockOptions) throws MappingException {

		final int joins = countEntityPersisters( associations );
		suffixes = BasicLoader.generateSuffixes( joins + 1 );

		JoinFragment ojf = mergeOuterJoins( associations );

		Select select = new Select( getDialect() )
				.setLockOptions( lockOptions )
				.setSelectClause(
						projection == null ?
								persister.selectFragment( alias, suffixes[joins] ) + selectString( associations ) :
								projection
				)
				.setFromClause(
						getDialect().appendLockHint( lockOptions, persister.fromTableFragment( alias ) ) +
								persister.fromJoinFragment( alias, true, true )
				)
				.setWhereClause( condition )
				.setOuterJoins(
						ojf.toFromFragmentString(),
						ojf.toWhereFragmentString() + getWhereFragment()
				)
				.setOrderByClause( orderBy( associations, orderBy ) )
				.setGroupByClause( groupBy );

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( getComment() );
		}
		sql = select.toStatementString();
	}

	protected String getWhereFragment() throws MappingException {
		// here we do not bother with the discriminator.
		return persister.whereJoinFragment(alias, true, true);
	}

	/**
	 * The superclass deliberately excludes collections
	 */
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config, CascadeStyle cascadeStyle) {
		return isJoinedFetchEnabledInMapping( config, type );
	}

	protected final boolean isJoinFetchEnabledByProfile(OuterJoinLoadable persister, PropertyPath path, int propertyNumber) {
		if ( !getLoadQueryInfluencers().hasEnabledFetchProfiles() ) {
			// perf optimization
			return false;
		}

		// ugh, this stuff has to be made easier...
		final String fullPath = path.getFullPath();
		String rootPropertyName = persister.getSubclassPropertyName( propertyNumber );
		int pos = fullPath.lastIndexOf( rootPropertyName );
		String relativePropertyPath = pos >= 0
				? fullPath.substring( pos )
				: rootPropertyName;
		String fetchRole = persister.getEntityName() + "." + relativePropertyPath;

		for ( String profileName : getLoadQueryInfluencers().getEnabledFetchProfileNames() ) {
			final FetchProfile profile = getFactory().getFetchProfile( profileName );
			final Fetch fetch = profile.getFetchByRole( fetchRole );
			if ( fetch != null && Fetch.Style.JOIN == fetch.getStyle() ) {
				return true;
			}
		}
		return false;
	}

	public abstract String getComment();

	@Override
    protected boolean isDuplicateAssociation(final String foreignKeyTable, final String[] foreignKeyColumns) {
		//disable a join back to this same association
		final boolean isSameJoin =
				persister.getTableName().equals( foreignKeyTable ) &&
						Arrays.equals( foreignKeyColumns, persister.getKeyColumnNames() );
		return isSameJoin ||
			super.isDuplicateAssociation(foreignKeyTable, foreignKeyColumns);
	}



	public final Loadable getPersister() {
		return persister;
	}

	public final String getAlias() {
		return alias;
	}
	
	/**
	 * For entities, orderings added by, for example, Criteria#addOrder need to come before the associations' @OrderBy
	 * values.  However, other sub-classes of JoinWalker (BasicCollectionJoinWalker, OneToManyJoinWalker, etc.)
	 * still need the other way around.  So, override here instead.  See HHH-7116.
	 */
	@Override
	protected String orderBy(final List associations, final String orderBy) {
		return mergeOrderings( orderBy, orderBy( associations ) );
	}

	public String toString() {
		return getClass().getName() + '(' + getPersister().getEntityName() + ')';
	}
}
