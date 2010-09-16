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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.hibernate.event.spi.DuplicationResolutionStrategy;

/**
 * Encapsulates the listeners regsitered for a particular event
 *
 * @author Steve Ebersole
 */
public class RegisteredEventListeners<T> {
	private EventType eventType;

	private Set<Class> deDupMarkers;
	private List<T> listeners;

	public RegisteredEventListeners(EventType eventType) {
		this.eventType = eventType;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void clear() {
		if ( deDupMarkers != null ) {
			deDupMarkers.clear();
		}
		if ( listeners != null ) {
			listeners.clear();
		}
	}

	/**
	 * "de-dup markers" is a mechanism to more finely control the notion of duplicates.
	 * <p/>
	 * For example, say you are registering listeners for an extension library.  This extension library
	 * could define a "marker interface" which indicates listeners related to it and add that here as a de-dup
	 * marker.  We would then only allow one instance that is an instance of this marker to be registered
	 * in the series of listeners for this event.
	 *
	 * @param marker
	 *
	 * @see org.hibernate.event.spi.DuplicationResolutionStrategy
	 */
	public void addDeDupMarker(Class marker) {
		if ( deDupMarkers == null ) {
			deDupMarkers = new HashSet<Class>();
		}
		deDupMarkers.add( marker );
	}

	@SuppressWarnings({ "unchecked" })
	public T[] getListenerArray() {
		if ( listeners == null ) {
			return (T[]) Array.newInstance( eventType.baseListenerInterface(), 0 );
		}
		else {
			final int size = listeners.size();
			return listeners.toArray( (T[]) Array.newInstance( eventType.baseListenerInterface(), size ) );
		}
	}

	void appendListener(T listener) {
		appendListener( listener, DuplicationResolutionStrategy.ERROR );
	}

	void appendListener(T listener, DuplicationResolutionStrategy dupStrategy) {
		if ( addListener( listener, dupStrategy ) ) {
			internalAppend( listener );
		}
	}

	void prependListener(T listener) {
		prependListener( listener, DuplicationResolutionStrategy.ERROR );
	}

	void prependListener(T listener, DuplicationResolutionStrategy dupStrategy) {
		if ( addListener( listener, dupStrategy ) ) {
			internalPrepend( listener );
		}
	}

	private boolean addListener(T listener, DuplicationResolutionStrategy duplicationResolutionStrategy) {
		boolean doAdd = true;
		if ( listeners == null ) {
			listeners = new ArrayList<T>();
			return true;
			// no need to do de-dup checks
		}
		else {
			for ( Class deDupMarker : deDupMarkers ) {
				if ( deDupMarker.isInstance( listener ) ) {
					final ListIterator<T> itr = listeners.listIterator();
					while ( itr.hasNext() ) {
						final T existingListener = itr.next();
						if ( deDupMarker.isInstance( existingListener ) ) {
							switch ( duplicationResolutionStrategy ) {
								case ERROR: {
									throw new EventListenerRegistrationException(
											"Duplicate event listener found for " + deDupMarker.getName()
									);
								}
								case REMOVE_ORIGINAL: {
									itr.remove();
								}
								case KEEP_ORIGINAL: {
									doAdd = false;
								}
								case REPLACE_ORIGINAL: {
									itr.set( listener );
									doAdd = false;
								}
							}
						}
					}
				}
			}
		}
		return doAdd;
	}

	private void internalPrepend(T listener) {
		checkAgainstBaseInterface( listener );
		listeners.add( 0, listener );
		addDeDupMarker( listener.getClass() );
	}

	private void checkAgainstBaseInterface(T listener) {
		if ( !eventType.baseListenerInterface().isInstance( listener ) ) {
			throw new EventListenerRegistrationException(
					"Listener did not implement expected interface [" + eventType.baseListenerInterface().getName() + "]"
			);
		}
	}

	private void internalAppend(T listener) {
		checkAgainstBaseInterface( listener );
		listeners.add( listener );
		addDeDupMarker( listener.getClass() );
	}
}
