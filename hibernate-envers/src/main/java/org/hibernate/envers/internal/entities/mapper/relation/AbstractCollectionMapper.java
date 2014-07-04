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
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public abstract class AbstractCollectionMapper<T> implements PropertyMapper {
	protected final CommonCollectionMapperData commonCollectionMapperData;
	protected final Class<? extends T> collectionClass;
	protected final boolean ordinalInId;
	protected final boolean revisionTypeInId;

	private final Constructor<? extends T> proxyConstructor;

	protected AbstractCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends T> collectionClass, Class<? extends T> proxyClass, boolean ordinalInId,
			boolean revisionTypeInId) {
		this.commonCollectionMapperData = commonCollectionMapperData;
		this.collectionClass = collectionClass;
		this.ordinalInId = ordinalInId;
		this.revisionTypeInId = revisionTypeInId;

		try {
			proxyConstructor = proxyClass.getConstructor( Initializor.class );
		}
		catch (NoSuchMethodException e) {
			throw new AuditException( e );
		}
	}

	protected abstract Collection getNewCollectionContent(PersistentCollection newCollection);

	protected abstract Collection getOldCollectionContent(Serializable oldCollection);

	/**
	 * Maps the changed collection element to the given map.
	 *
	 * @param idData Map to which composite-id data should be added.
	 * @param data Where to map the data.
	 * @param changed The changed collection element to map.
	 */
	protected abstract void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object changed);

	/**
	 * Creates map for storing identifier data. Ordinal parameter guarantees uniqueness of primary key.
	 * Composite primary key cannot contain embeddable properties since they might be nullable.
	 *
	 * @param ordinal Iteration ordinal.
	 *
	 * @return Map for holding identifier data.
	 */
	protected Map<String, Object> createIdMap(int ordinal) {
		final Map<String, Object> idMap = new HashMap<String, Object>();
		if ( ordinalInId ) {
			idMap.put( commonCollectionMapperData.getVerEntCfg().getEmbeddableSetOrdinalPropertyName(), ordinal );
		}
		return idMap;
	}

	private void addCollectionChanges(
			SessionImplementor session, List<PersistentCollectionChangeData> collectionChanges,
			Set<Object> changed, RevisionType revisionType, Serializable id) {
		int ordinal = 0;

		for ( Object changedObj : changed ) {
			final Map<String, Object> entityData = new HashMap<String, Object>();
			final Map<String, Object> originalId = createIdMap( ordinal++ );
			entityData.put( commonCollectionMapperData.getVerEntCfg().getOriginalIdPropName(), originalId );

			collectionChanges.add(
					new PersistentCollectionChangeData(
							commonCollectionMapperData.getVersionsMiddleEntityName(), entityData, changedObj
					)
			);
			// Mapping the collection owner's id.
			commonCollectionMapperData.getReferencingIdData().getPrefixedMapper().mapToMapFromId( originalId, id );

			// Mapping collection element and index (if present).
			mapToMapFromObject( session, originalId, entityData, changedObj );

			(revisionTypeInId ? originalId : entityData).put(
					commonCollectionMapperData.getVerEntCfg()
							.getRevisionTypePropName(), revisionType
			);
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id) {
		if ( !commonCollectionMapperData.getCollectionReferencingPropertyData().getName().equals(
				referencingPropertyName
		) ) {
			return null;
		}

		final List<PersistentCollectionChangeData> collectionChanges = new ArrayList<PersistentCollectionChangeData>();

		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		final Set<Object> added = new HashSet<Object>();
		if ( newColl != null ) {
			added.addAll( newCollection );
		}
		// Re-hashing the old collection as the hash codes of the elements there may have changed, and the
		// removeAll in AbstractSet has an implementation that is hashcode-change sensitive (as opposed to addAll).
		if ( oldColl != null ) {
			added.removeAll( new HashSet( oldCollection ) );
		}

		addCollectionChanges( session, collectionChanges, added, RevisionType.ADD, id );

		final Set<Object> deleted = new HashSet<Object>();
		if ( oldColl != null ) {
			deleted.addAll( oldCollection );
		}
		// The same as above - re-hashing new collection.
		if ( newColl != null ) {
			deleted.removeAll( new HashSet( newCollection ) );
		}

		addCollectionChanges( session, collectionChanges, deleted, RevisionType.DEL, id );

		return collectionChanges;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		// Changes are mapped in the "mapCollectionChanges" method.
		return false;
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
		if ( propertyData.isUsingModifiedFlag() ) {
			if ( isNotPersistentCollection( newObj ) || isNotPersistentCollection( oldObj ) ) {
				// Compare POJOs.
				data.put( propertyData.getModifiedFlagPropertyName(), !Tools.objectsEqual( newObj, oldObj ) );
			}
			else if ( isFromNullToEmptyOrFromEmptyToNull( (PersistentCollection) newObj, (Serializable) oldObj ) ) {
				data.put( propertyData.getModifiedFlagPropertyName(), true );
			}
			else {
				final List<PersistentCollectionChangeData> changes = mapCollectionChanges(
						session,
						commonCollectionMapperData.getCollectionReferencingPropertyData().getName(),
						(PersistentCollection) newObj,
						(Serializable) oldObj,
						null
				);
				data.put( propertyData.getModifiedFlagPropertyName(), !changes.isEmpty() );
			}
		}
	}

	private boolean isNotPersistentCollection(Object obj) {
		return obj != null && !(obj instanceof PersistentCollection);
	}

	private boolean isFromNullToEmptyOrFromEmptyToNull(PersistentCollection newColl, Serializable oldColl) {
		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		return oldCollection == null && newCollection != null && newCollection.isEmpty()
				|| newCollection == null && oldCollection != null && oldCollection.isEmpty();
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
		if ( propertyData.isUsingModifiedFlag() ) {
			data.put(
					propertyData.getModifiedFlagPropertyName(),
					propertyData.getName().equals( collectionPropertyName )
			);
		}
	}

	protected abstract Initializor<T> getInitializor(
			AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader, Object primaryKey,
			Number revision, boolean removed);

	@Override
	public void mapToEntityFromMap(
			AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
			AuditReaderImplementor versionsReader, Number revision) {
		final Setter setter = ReflectionTools.getSetter(
				obj.getClass(),
				commonCollectionMapperData.getCollectionReferencingPropertyData()
		);
		try {
			setter.set(
					obj,
					proxyConstructor.newInstance(
							getInitializor(
									verCfg, versionsReader, primaryKey, revision,
									RevisionType.DEL.equals(
											data.get(
													verCfg.getAuditEntCfg()
															.getRevisionTypePropName()
											)
									)
							)
					),
					null
			);
		}
		catch (InstantiationException e) {
			throw new AuditException( e );
		}
		catch (IllegalAccessException e) {
			throw new AuditException( e );
		}
		catch (InvocationTargetException e) {
			throw new AuditException( e );
		}
	}
}
