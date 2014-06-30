/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan.build.internal.spaces.QuerySpaceHelper;
import org.hibernate.loader.plan.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public class CollectionAttributeFetchImpl extends AbstractCollectionReference implements CollectionAttributeFetch {
	private final ExpandingFetchSource fetchSource;
	private final AttributeDefinition fetchedAttribute;
	private final FetchStrategy fetchStrategy;

	public CollectionAttributeFetchImpl(
			ExpandingFetchSource fetchSource,
			AssociationAttributeDefinition fetchedAttribute,
			FetchStrategy fetchStrategy,
			ExpandingCollectionQuerySpace collectionQuerySpace) {
		super(
				collectionQuerySpace,
				fetchSource.getPropertyPath().append( fetchedAttribute.getName() ),
				QuerySpaceHelper.INSTANCE.shouldIncludeJoin( fetchStrategy )

		);

		this.fetchSource = fetchSource;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchStrategy = fetchStrategy;
	}

	@Override
	public FetchSource getSource() {
		return fetchSource;
	}

	@Override
	public CollectionType getFetchedType() {
		return (CollectionType) fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public String getAdditionalJoinConditions() {
		// only pertinent for HQL...
		return null;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}






	@Override
	public String[] toSqlSelectFragments(String alias) {
		return null;
	}

//	@Override
//	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		//To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public Object resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		return null;
//	}
//
//	@Override
//	public void read(ResultSet resultSet, ResultSetProcessingContext context, Object owner) throws SQLException {
//		final Serializable collectionRowKey = (Serializable) getCollectionPersister().readKey(
//				resultSet,
//				context.getAliasResolutionContext().resolveAliases( this ).getCollectionColumnAliases().getSuffixedKeyAliases(),
//				context.getSession()
//		);
//
//		final PersistenceContext persistenceContext = context.getSession().getPersistenceContext();
//
//		if ( collectionRowKey == null ) {
//			// we did not find a collection element in the result set, so we
//			// ensure that a collection is created with the owner's identifier,
//			// since what we have is an empty collection
//			final EntityKey ownerEntityKey = findOwnerEntityKey( context );
//			if ( ownerEntityKey == null ) {
//				// should not happen
//				throw new IllegalStateException(
//						"Could not locate owner's EntityKey during attempt to read collection element fro JDBC row : " +
//								getPropertyPath().getFullPath()
//				);
//			}
//
//			if ( log.isDebugEnabled() ) {
//				log.debugf(
//						"Result set contains (possibly empty) collection: %s",
//						MessageHelper.collectionInfoString(
//								getCollectionPersister(),
//								ownerEntityKey,
//								context.getSession().getFactory()
//						)
//				);
//			}
//
//			persistenceContext.getLoadContexts()
//					.getCollectionLoadContext( resultSet )
//					.getLoadingCollection( getCollectionPersister(), ownerEntityKey );
//		}
//		else {
//			// we found a collection element in the result set
//			if ( log.isDebugEnabled() ) {
//				log.debugf(
//						"Found row of collection: %s",
//						MessageHelper.collectionInfoString(
//								getCollectionPersister(),
//								collectionRowKey,
//								context.getSession().getFactory()
//						)
//				);
//			}
//
//			PersistentCollection rowCollection = persistenceContext.getLoadContexts()
//					.getCollectionLoadContext( resultSet )
//					.getLoadingCollection( getCollectionPersister(), collectionRowKey );
//
//			final CollectionAliases descriptor = context.getAliasResolutionContext().resolveAliases( this ).getCollectionColumnAliases();
//
//			if ( rowCollection != null ) {
//				final Object element = rowCollection.readFrom( resultSet, getCollectionPersister(), descriptor, owner );
//
//				if ( getElementGraph() != null ) {
//					for ( Fetch fetch : getElementGraph().getFetches() ) {
//						fetch.read( resultSet, context, element );
//					}
//				}
//			}
//		}
//	}
//
//	private EntityKey findOwnerEntityKey(ResultSetProcessingContext context) {
//		return context.getProcessingState( findOwnerEntityReference( getOwner() ) ).getEntityKey();
//	}
//
//	private EntityReference findOwnerEntityReference(FetchOwner owner) {
//		return Helper.INSTANCE.findOwnerEntityReference( owner );
//	}

	@Override
	public AttributeDefinition getFetchedAttributeDefinition() {
		return fetchedAttribute;
	}
}
