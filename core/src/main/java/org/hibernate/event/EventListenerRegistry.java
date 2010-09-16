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
package org.hibernate.event;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.event.def.DefaultAutoFlushEventListener;
import org.hibernate.event.def.DefaultDeleteEventListener;
import org.hibernate.event.def.DefaultDirtyCheckEventListener;
import org.hibernate.event.def.DefaultEvictEventListener;
import org.hibernate.event.def.DefaultFlushEntityEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.event.def.DefaultInitializeCollectionEventListener;
import org.hibernate.event.def.DefaultLoadEventListener;
import org.hibernate.event.def.DefaultLockEventListener;
import org.hibernate.event.def.DefaultMergeEventListener;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.hibernate.event.def.DefaultPersistOnFlushEventListener;
import org.hibernate.event.def.DefaultPostLoadEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;
import org.hibernate.event.def.DefaultRefreshEventListener;
import org.hibernate.event.def.DefaultReplicateEventListener;
import org.hibernate.event.def.DefaultSaveEventListener;
import org.hibernate.event.def.DefaultSaveOrUpdateCopyEventListener;
import org.hibernate.event.def.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.def.DefaultUpdateEventListener;
import org.hibernate.event.spi.DuplicationResolutionStrategy;
import org.hibernate.util.ReflectHelper;

import static org.hibernate.event.EventType.*;

/**
 * Registry of event listeners
 *
 * @author Steve Ebersole
 */
public class EventListenerRegistry implements Serializable {
	private Map<String,Object> listenerClassToInstanceMap = new HashMap<String, Object>();

	private Map<EventType,RegisteredEventListeners> registeredEventListenersMap = prepareListenerMap();

	public RegisteredEventListeners getRegisteredEventListeners(String eventName) {
		if ( eventName == null ) {
			throw new HibernateException( "event name cannot be null" );
		}
		final EventType eventType = EventType.resolveEventTypeByName( eventName );
		return registeredEventListenersMap.get( eventType );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> RegisteredEventListeners<T> getRegisteredEventListeners(EventType<T> eventType) {
		RegisteredEventListeners<T> listeners = registeredEventListenersMap.get( eventType );
		if ( listeners == null ) {
			throw new HibernateException( "Unable to find listeners for type [" + eventType.eventName() + "]" );
		}
		return listeners;
	}

	public <T> void setListeners(EventType<T> type, T... listeners) {
		RegisteredEventListeners<T> registeredListeners = getRegisteredEventListeners( type );
		registeredListeners.clear();
		if ( listeners != null ) {
			for ( int i = 0, max = listeners.length; i < max; i++ ) {
				registeredListeners.appendListener( listeners[i] );
			}
		}
	}

	/**
	 *
	 * @param type
	 * @param listenerClassName
	 */
	@SuppressWarnings({ "unchecked" })
	public void appendListener(String eventName, String listenerClassName) {
		if ( listenerClassName != null ) {
			getRegisteredEventListeners( eventName ).appendListener( resolveListenerInstance( listenerClassName ) );
		}
	}

	public <T> void appendListener(EventType<T> type, T listener) {
		appendListener( type, listener, DuplicationResolutionStrategy.ERROR );
	}

	public <T> void appendListener(EventType<T> type, T listener, DuplicationResolutionStrategy dupStrategy) {
		getRegisteredEventListeners( type ).appendListener( listener, dupStrategy );
	}

	/**
	 *
	 * @param type
	 * @param listenerClassName
	 */
	@SuppressWarnings({ "unchecked" })
	public void prependListener(String type, String listenerClassName) {
		if ( listenerClassName != null ) {
			getRegisteredEventListeners( type ).prependListener( resolveListenerInstance( listenerClassName ) );
		}
	}

	public <T> void prependListener(EventType<T> type, T listener) {
		prependListener( type, listener, DuplicationResolutionStrategy.ERROR );
	}

	public <T> void prependListener(EventType<T> type, T listener, DuplicationResolutionStrategy dupStrategy) {
		getRegisteredEventListeners( type ).prependListener( listener, dupStrategy );
	}

	private Object resolveListenerInstance(String listenerClassName) {
		Object listenerInstance = listenerClassToInstanceMap.get( listenerClassName );
		if ( listenerInstance == null ) {
			listenerInstance = instantiateListener( listenerClassName );
			listenerClassToInstanceMap.put( listenerClassName, listenerInstance );
		}
		return listenerInstance;

	}

	private Object instantiateListener(String listenerClassName) {
		try {
			return ReflectHelper.classForName( listenerClassName ).newInstance();
		}
		catch ( Exception e ) {
			throw new EventListenerRegistrationException(
					"Unable to instantiate specified event listener class: " + listenerClassName,
					e
			);
		}
	}

	private static Map<EventType,RegisteredEventListeners> prepareListenerMap() {
		final Map<EventType,RegisteredEventListeners> workMap = new HashMap<EventType, RegisteredEventListeners>();

		// auto-flush listeners
		prepareListeners(
				AUTO_FLUSH,
				new DefaultAutoFlushEventListener(),
				workMap
		);

		// create listeners
		prepareListeners(
				PERSIST,
				new DefaultPersistEventListener(),
				workMap
		);

		// create-onflush listeners
		prepareListeners(
				PERSIST_ONFLUSH,
				new DefaultPersistOnFlushEventListener(),
				workMap
		);

		// delete listeners
		prepareListeners(
				DELETE,
				new DefaultDeleteEventListener(),
				workMap
		);

		// dirty-check listeners
		prepareListeners(
				DIRTY_CHECK,
				new DefaultDirtyCheckEventListener(),
				workMap
		);

		// evict listeners
		prepareListeners(
				EVICT,
				new DefaultEvictEventListener(),
				workMap
		);

		// flush listeners
		prepareListeners(
				FLUSH,
				new DefaultFlushEventListener(),
				workMap
		);

		// flush-entity listeners
		prepareListeners(
				FLUSH_ENTITY,
				new DefaultFlushEntityEventListener(),
				workMap
		);

		// load listeners
		prepareListeners(
				LOAD,
				new DefaultLoadEventListener(),
				workMap
		);

		// load-collection listeners
		prepareListeners(
				INIT_COLLECTION,
				new DefaultInitializeCollectionEventListener(),
				workMap
		);

		// lock listeners
		prepareListeners(
				LOCK,
				new DefaultLockEventListener(),
				workMap
		);

		// merge listeners
		prepareListeners(
				MERGE,
				new DefaultMergeEventListener(),
				workMap
		);

		// pre-collection-recreate listeners
		prepareListeners(
				PRE_COLLECTION_RECREATE,
				workMap
		);

		// pre-collection-remove listeners
		prepareListeners(
				PRE_COLLECTION_REMOVE,
				workMap
		);

		// pre-collection-update listeners
		prepareListeners(
				PRE_COLLECTION_UPDATE,
				workMap
		);

		// pre-delete listeners
		prepareListeners(
				PRE_DELETE,
				workMap
		);

		// pre-insert listeners
		prepareListeners(
				PRE_INSERT,
				workMap
		);

		// pre-load listeners
		prepareListeners(
				PRE_LOAD,
				new DefaultPreLoadEventListener(),
				workMap
		);

		// pre-update listeners
		prepareListeners(
				PRE_UPDATE,
				workMap
		);

		// post-collection-recreate listeners
		prepareListeners(
				POST_COLLECTION_RECREATE,
				workMap
		);

		// post-collection-remove listeners
		prepareListeners(
				POST_COLLECTION_REMOVE,
				workMap
		);

		// post-collection-update listeners
		prepareListeners(
				POST_COLLECTION_UPDATE,
				workMap
		);

		// post-commit-delete listeners
		prepareListeners(
				POST_COMMIT_DELETE,
				workMap
		);

		// post-commit-insert listeners
		prepareListeners(
				POST_COMMIT_INSERT,
				workMap
		);

		// post-commit-update listeners
		prepareListeners(
				POST_COMMIT_UPDATE,
				workMap
		);

		// post-delete listeners
		prepareListeners(
				POST_DELETE,
				workMap
		);

		// post-insert listeners
		prepareListeners(
				POST_INSERT,
				workMap
		);

		// post-load listeners
		prepareListeners(
				POST_LOAD,
				new DefaultPostLoadEventListener(),
				workMap
		);

		// post-update listeners
		prepareListeners(
				POST_UPDATE,
				workMap
		);

		// update listeners
		prepareListeners(
				UPDATE,
				new DefaultUpdateEventListener(),
				workMap
		);

		// refresh listeners
		prepareListeners(
				REFRESH,
				new DefaultRefreshEventListener(),
				workMap
		);

		// replicate listeners
		prepareListeners(
				REPLICATE,
				new DefaultReplicateEventListener(),
				workMap
		);

		// save listeners
		prepareListeners(
				SAVE,
				new DefaultSaveEventListener(),
				workMap
		);

		// save-update listeners
		prepareListeners(
				SAVE_UPDATE,
				new DefaultSaveOrUpdateEventListener(),
				workMap
		);

		// save-update-copy listeners
		prepareListeners(
				SAVE_UPDATE_COPY,
				new DefaultSaveOrUpdateCopyEventListener(),
				workMap
		);

		return Collections.unmodifiableMap( workMap );
	}

	private static <T> void prepareListeners(EventType<T> type, Map<EventType,RegisteredEventListeners> map) {
		prepareListeners( type, null, map  );
	}

	private static <T> void prepareListeners(EventType<T> type, T defaultListener, Map<EventType,RegisteredEventListeners> map) {
		final RegisteredEventListeners<T> listeners = new RegisteredEventListeners<T>( type );
		if ( defaultListener != null ) {
			listeners.appendListener( defaultListener );
		}
		map.put( type, listeners  );
	}

}
