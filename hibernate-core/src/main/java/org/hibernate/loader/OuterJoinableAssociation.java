/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.loader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;

/**
 * Part of the Hibernate SQL rendering internals.  This class represents
 * a joinable association.
 *
 * @author Gavin King
 */
public final class OuterJoinableAssociation {
	private final PropertyPath propertyPath;
	private final AssociationType joinableType;
	private final Joinable joinable;
	private final String lhsAlias; // belong to other persister
	private final String[] lhsColumns; // belong to other persister
	private final String rhsAlias;
	private final String[] rhsColumns;
	private final JoinType joinType;
	private final String on;
	private final Map enabledFilters;
	private final boolean hasRestriction;

	public static OuterJoinableAssociation createRoot(
			AssociationType joinableType,
			String alias,
			SessionFactoryImplementor factory) {
		return new OuterJoinableAssociation(
				new PropertyPath(),
				joinableType,
				null,
				null,
				alias,
				JoinType.LEFT_OUTER_JOIN,
				null,
				false,
				factory,
				Collections.EMPTY_MAP
		);
	}

	public OuterJoinableAssociation(
			PropertyPath propertyPath,
			AssociationType joinableType,
			String lhsAlias,
			String[] lhsColumns,
			String rhsAlias,
			JoinType joinType,
			String withClause,
			boolean hasRestriction,
			SessionFactoryImplementor factory,
			Map enabledFilters) throws MappingException {
		this.propertyPath = propertyPath;
		this.joinableType = joinableType;
		this.lhsAlias = lhsAlias;
		this.lhsColumns = lhsColumns;
		this.rhsAlias = rhsAlias;
		this.joinType = joinType;
		this.joinable = joinableType.getAssociatedJoinable(factory);
		this.rhsColumns = JoinHelper.getRHSColumnNames(joinableType, factory);
		this.on = joinableType.getOnCondition(rhsAlias, factory, enabledFilters)
			+ ( withClause == null || withClause.trim().length() == 0 ? "" : " and ( " + withClause + " )" );
		this.hasRestriction = hasRestriction;
		this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
	}

	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public String getLhsAlias() {
		return lhsAlias;
	}

	public String getRHSAlias() {
		return rhsAlias;
	}

	public String getRhsAlias() {
		return rhsAlias;
	}

	private boolean isOneToOne() {
		if ( joinableType.isEntityType() )  {
			EntityType etype = (EntityType) joinableType;
			return etype.isOneToOne() /*&& etype.isReferenceToPrimaryKey()*/;
		}
		else {
			return false;
		}
	}
	
	public AssociationType getJoinableType() {
		return joinableType;
	}
	
	public String getRHSUniqueKeyName() {
		return joinableType.getRHSUniqueKeyPropertyName();
	}

	public boolean isCollection() {
		return joinableType.isCollectionType();
	}

	public Joinable getJoinable() {
		return joinable;
	}

	public boolean hasRestriction() {
		return hasRestriction;
	}

	public int getOwner(final List associations) {
		if ( isOneToOne() || isCollection() ) {
			return getPosition(lhsAlias, associations);
		}
		else {
			return -1;
		}
	}

	/**
	 * Get the position of the join with the given alias in the
	 * list of joins
	 */
	private static int getPosition(String lhsAlias, List associations) {
		int result = 0;
		for ( int i=0; i<associations.size(); i++ ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) associations.get(i);
			if ( oj.getJoinable().consumesEntityAlias() /*|| oj.getJoinable().consumesCollectionAlias() */ ) {
				if ( oj.rhsAlias.equals(lhsAlias) ) return result;
				result++;
			}
		}
		return -1;
	}

	public void addJoins(JoinFragment outerjoin) throws MappingException {
		outerjoin.addJoin(
			joinable.getTableName(),
			rhsAlias,
			lhsColumns,
			rhsColumns,
			joinType,
			on
		);
		outerjoin.addJoins(
			joinable.fromJoinFragment(rhsAlias, false, true),
			joinable.whereJoinFragment(rhsAlias, false, true)
		);
	}

	public void validateJoin(String path) throws MappingException {
		if ( rhsColumns==null || lhsColumns==null
				|| lhsColumns.length!=rhsColumns.length || lhsColumns.length==0 ) {
			throw new MappingException("invalid join columns for association: " + path);
		}
	}

	public boolean isManyToManyWith(OuterJoinableAssociation other) {
		if ( joinable.isCollection() ) {
			QueryableCollection persister = ( QueryableCollection ) joinable;
			if ( persister.isManyToMany() ) {
				return persister.getElementType() == other.getJoinableType();
			}
		}
		return false;
	}

	public void addManyToManyJoin(JoinFragment outerjoin, QueryableCollection collection) throws MappingException {
		String manyToManyFilter = collection.getManyToManyFilterFragment( rhsAlias, enabledFilters );
		String condition = "".equals( manyToManyFilter )
				? on
				: "".equals( on )
						? manyToManyFilter
						: on + " and " + manyToManyFilter;
		outerjoin.addJoin(
		        joinable.getTableName(),
		        rhsAlias,
		        lhsColumns,
		        rhsColumns,
		        joinType,
		        condition
		);
		outerjoin.addJoins(
			joinable.fromJoinFragment(rhsAlias, false, true),
			joinable.whereJoinFragment(rhsAlias, false, true)
		);
	}
}
