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
package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

import org.jboss.logging.Logger;

/**
 * Functionality relating to the Hibernate two-phase loading process, that may be reused by persisters
 * that do not use the Loader framework
 *
 * @author Gavin King
 */
public final class TwoPhaseLoad {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TwoPhaseLoad.class.getName()
	);

	private TwoPhaseLoad() {
	}

	/**
	 * Register the "hydrated" state of an entity instance, after the first step of 2-phase loading.
	 *
	 * Add the "hydrated state" (an array) of an uninitialized entity to the session. We don't try
	 * to resolve any associations yet, because there might be other entities waiting to be
	 * read from the JDBC result set we are currently processing
	 *
	 * @param persister The persister for the hydrated entity
	 * @param id The entity identifier
	 * @param values The entity values
	 * @param rowId The rowId for the entity
	 * @param object An optional instance for the entity being loaded
	 * @param lockMode The lock mode
	 * @param lazyPropertiesAreUnFetched Whether properties defined as lazy are yet un-fetched
	 * @param session The Session
	 */
	public static void postHydrate(
			final EntityPersister persister,
			final Serializable id,
			final Object[] values,
			final Object rowId,
			final Object object,
			final LockMode lockMode,
			final boolean lazyPropertiesAreUnFetched,
			final SessionImplementor session) {
		final Object version = Versioning.getVersion( values, persister );
		session.getPersistenceContext().addEntry(
				object,
				Status.LOADING,
				values,
				rowId,
				id,
				version,
				lockMode,
				true,
				persister,
				false,
				lazyPropertiesAreUnFetched
			);

		if ( version != null && LOG.isTraceEnabled() ) {
			final String versionStr = persister.isVersioned()
					? persister.getVersionType().toLoggableString( version, session.getFactory() )
					: "null";
			LOG.tracef( "Version: %s", versionStr );
		}
	}

	/**
	 * Perform the second step of 2-phase load. Fully initialize the entity
	 * instance.
	 * <p/>
	 * After processing a JDBC result set, we "resolve" all the associations
	 * between the entities which were instantiated and had their state
	 * "hydrated" into an array
	 *
	 * @param entity The entity being loaded
	 * @param readOnly Is the entity being loaded as read-only
	 * @param session The Session
	 * @param preLoadEvent The (re-used) pre-load event
	 */
	public static void initializeEntity(
			final Object entity,
			final boolean readOnly,
			final SessionImplementor session,
			final PreLoadEvent preLoadEvent) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityEntry entityEntry = persistenceContext.getEntry( entity );
		if ( entityEntry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}
		doInitializeEntity( entity, entityEntry, readOnly, session, preLoadEvent );
	}

	private static void doInitializeEntity(
			final Object entity,
			final EntityEntry entityEntry,
			final boolean readOnly,
			final SessionImplementor session,
			final PreLoadEvent preLoadEvent) throws HibernateException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityPersister persister = entityEntry.getPersister();
		final Serializable id = entityEntry.getId();
		final Object[] hydratedState = entityEntry.getLoadedState();

		final boolean debugEnabled = LOG.isDebugEnabled();
		if ( debugEnabled ) {
			LOG.debugf(
					"Resolving associations for %s",
					MessageHelper.infoString( persister, id, session.getFactory() )
			);
		}

		final Type[] types = persister.getPropertyTypes();
		for ( int i = 0; i < hydratedState.length; i++ ) {
			final Object value = hydratedState[i];
			if ( value!=LazyPropertyInitializer.UNFETCHED_PROPERTY && value!=BackrefPropertyAccessor.UNKNOWN ) {
				hydratedState[i] = types[i].resolve( value, session, entity );
			}
		}

		//Must occur after resolving identifiers!
		if ( session.isEventSource() ) {
			preLoadEvent.setEntity( entity ).setState( hydratedState ).setId( id ).setPersister( persister );

			final EventListenerGroup<PreLoadEventListener> listenerGroup = session
					.getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.PRE_LOAD );
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}

		persister.setPropertyValues( entity, hydratedState );

		final SessionFactoryImplementor factory = session.getFactory();
		if ( persister.hasCache() && session.getCacheMode().isPutEnabled() ) {

			if ( debugEnabled ) {
				LOG.debugf(
						"Adding entity to second-level cache: %s",
						MessageHelper.infoString( persister, id, session.getFactory() )
				);
			}

			final Object version = Versioning.getVersion( hydratedState, persister );
			final CacheEntry entry = persister.buildCacheEntry( entity, hydratedState, version, session );
			final CacheKey cacheKey = session.generateCacheKey( id, persister.getIdentifierType(), persister.getRootEntityName() );

			// explicit handling of caching for rows just inserted and then somehow forced to be read
			// from the database *within the same transaction*.  usually this is done by
			// 		1) Session#refresh, or
			// 		2) Session#clear + some form of load
			//
			// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
			if ( session.getPersistenceContext().wasInsertedDuringTransaction( persister, id ) ) {
				persister.getCacheAccessStrategy().update(
						cacheKey,
						persister.getCacheEntryStructure().structure( entry ),
						version,
						version
				);
			}
			else {
				try {
					session.getEventListenerManager().cachePutStart();
					final boolean put = persister.getCacheAccessStrategy().putFromLoad(
							cacheKey,
							persister.getCacheEntryStructure().structure( entry ),
							session.getTimestamp(),
							version,
							useMinimalPuts( session, entityEntry )
					);

					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatisticsImplementor().secondLevelCachePut( persister.getCacheAccessStrategy().getRegion().getName() );
					}
				}
				finally {
					session.getEventListenerManager().cachePutEnd();
				}
			}
		}

		if ( persister.hasNaturalIdentifier() ) {
			persistenceContext.getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					persister,
					id,
					persistenceContext.getNaturalIdHelper().extractNaturalIdValues( hydratedState, persister )
			);
		}

		boolean isReallyReadOnly = readOnly;
		if ( !persister.isMutable() ) {
			isReallyReadOnly = true;
		}
		else {
			final Object proxy = persistenceContext.getProxy( entityEntry.getEntityKey() );
			if ( proxy != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				isReallyReadOnly = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().isReadOnly();
			}
		}
		if ( isReallyReadOnly ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			TypeHelper.deepCopy(
					hydratedState,
					persister.getPropertyTypes(),
					persister.getPropertyUpdateability(),
					//after setting values to object
					hydratedState,
					session
			);
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}

		persister.afterInitialize(
				entity,
				entityEntry.isLoadedWithLazyPropertiesUnfetched(),
				session
		);

		if ( debugEnabled ) {
			LOG.debugf(
					"Done materializing entity %s",
					MessageHelper.infoString( persister, id, session.getFactory() )
			);
		}

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().loadEntity( persister.getEntityName() );
		}
	}
	
	/**
	 * PostLoad cannot occur during initializeEntity, as that call occurs *before*
	 * the Set collections are added to the persistence context by Loader.
	 * Without the split, LazyInitializationExceptions can occur in the Entity's
	 * postLoad if it acts upon the collection.
	 *
	 * HHH-6043
	 * 
	 * @param entity The entity
	 * @param session The Session
	 * @param postLoadEvent The (re-used) post-load event
	 */
	public static void postLoad(
			final Object entity,
			final SessionImplementor session,
			final PostLoadEvent postLoadEvent) {
		
		if ( session.isEventSource() ) {
			final PersistenceContext persistenceContext
					= session.getPersistenceContext();
			final EntityEntry entityEntry = persistenceContext.getEntry( entity );

			postLoadEvent.setEntity( entity ).setId( entityEntry.getId() ).setPersister( entityEntry.getPersister() );

			final EventListenerGroup<PostLoadEventListener> listenerGroup = session.getFactory()
							.getServiceRegistry()
							.getService( EventListenerRegistry.class )
							.getEventListenerGroup( EventType.POST_LOAD );
			for ( PostLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPostLoad( postLoadEvent );
			}
		}
	}

	private static boolean useMinimalPuts(SessionImplementor session, EntityEntry entityEntry) {
		return ( session.getFactory().getSettings().isMinimalPutsEnabled()
				&& session.getCacheMode()!=CacheMode.REFRESH )
				|| ( entityEntry.getPersister().hasLazyProperties()
				&& entityEntry.isLoadedWithLazyPropertiesUnfetched()
				&& entityEntry.getPersister().isLazyPropertiesCacheable() );
	}

	/**
	 * Add an uninitialized instance of an entity class, as a placeholder to ensure object
	 * identity. Must be called before <tt>postHydrate()</tt>.
	 *
	 * Create a "temporary" entry for a newly instantiated entity. The entity is uninitialized,
	 * but we need the mapping from id to instance in order to guarantee uniqueness.
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param persister The entity persister
	 * @param lockMode The lock mode
	 * @param lazyPropertiesAreUnFetched Are lazy properties still un-fetched?
	 * @param session The Session
	 */
	public static void addUninitializedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final boolean lazyPropertiesAreUnFetched,
			final SessionImplementor session) {
		session.getPersistenceContext().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				null,
				lockMode,
				true,
				persister,
				false,
				lazyPropertiesAreUnFetched
		);
	}

	/**
	 * Same as {@link #addUninitializedEntity}, but here for an entity from the second level cache
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param persister The entity persister
	 * @param lockMode The lock mode
	 * @param lazyPropertiesAreUnFetched Are lazy properties still un-fetched?
	 * @param version The version
	 * @param session The Session
	 */
	public static void addUninitializedCachedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final boolean lazyPropertiesAreUnFetched,
			final Object version,
			final SessionImplementor session) {
		session.getPersistenceContext().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				version,
				lockMode,
				true,
				persister,
				false,
				lazyPropertiesAreUnFetched
			);
	}
}
