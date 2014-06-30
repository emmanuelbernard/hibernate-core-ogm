/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CascadingActions {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			CascadingAction.class.getName()
	);

	/**
	 * Disallow instantiation
	 */
	private CascadingActions() {
	}

	/**
	 * @see org.hibernate.Session#delete(Object)
	 */
	public static final CascadingAction DELETE = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled) {
			LOG.tracev( "Cascading to delete: {0}", entityName );
			session.delete( entityName, child, isCascadeDeleteEnabled, (Set) anything );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// delete does cascade to uninitialized collections
			return getAllElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should be deleted during delete
			return true;
		}

		@Override
		public String toString() {
			return "ACTION_DELETE";
		}
	};

	/**
	 * @see org.hibernate.Session#lock(Object, org.hibernate.LockMode)
	 */
	public static final CascadingAction LOCK = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled) {
			LOG.tracev( "Cascading to lock: {0}", entityName );
			LockMode lockMode = LockMode.NONE;
			LockOptions lr = new LockOptions();
			if ( anything instanceof LockOptions ) {
				LockOptions lockOptions = (LockOptions) anything;
				lr.setTimeOut( lockOptions.getTimeOut() );
				lr.setScope( lockOptions.getScope() );
				if ( lockOptions.getScope() ) {
					lockMode = lockOptions.getLockMode();
				}
			}
			lr.setLockMode( lockMode );
			session.buildLockRequest( lr ).lock( entityName, child );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// lock doesn't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			//TODO: should orphans really be deleted during lock???
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_LOCK";
		}
	};

	/**
	 * @see org.hibernate.Session#refresh(Object)
	 */
	public static final CascadingAction REFRESH = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to refresh: {0}", entityName );
			session.refresh( entityName, child, (Map) anything );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// refresh doesn't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_REFRESH";
		}
	};

	/**
	 * @see org.hibernate.Session#evict(Object)
	 */
	public static final CascadingAction EVICT = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to evict: {0}", entityName );
			session.evict( child );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// evicts don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_EVICT";
		}
	};

	/**
	 * @see org.hibernate.Session#saveOrUpdate(Object)
	 */
	public static final CascadingAction SAVE_UPDATE = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to save or update: {0}", entityName );
			session.saveOrUpdate( entityName, child );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// saves / updates don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should be deleted during save/update
			return true;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_SAVE_UPDATE";
		}
	};

	/**
	 * @see org.hibernate.Session#merge(Object)
	 */
	public static final CascadingAction MERGE = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to merge: {0}", entityName );
			session.merge( entityName, child, (Map) anything );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// merges don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should not be deleted during merge??
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_MERGE";
		}
	};

	/**
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction PERSIST = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to persist: {0}" + entityName );
			session.persist( entityName, child, (Map) anything );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// persists don't cascade to uninitialized collections
			return getAllElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_PERSIST";
		}
	};

	/**
	 * Execute persist during flush time
	 *
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction PERSIST_ON_FLUSH = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to persist on flush: {0}", entityName );
			session.persistOnFlush( entityName, child, (Map) anything );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// persists don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return true;
		}

		@Override
		public boolean requiresNoCascadeChecking() {
			return true;
		}

		@Override
		public void noCascade(
				EventSource session,
				Object child,
				Object parent,
				EntityPersister persister,
				int propertyIndex) {
			if ( child == null ) {
				return;
			}
			Type type = persister.getPropertyTypes()[propertyIndex];
			if ( type.isEntityType() ) {
				String childEntityName = ((EntityType) type).getAssociatedEntityName( session.getFactory() );

				if ( !isInManagedState( child, session )
						&& !(child instanceof HibernateProxy) //a proxy cannot be transient and it breaks ForeignKeys.isTransient
						&& ForeignKeys.isTransient( childEntityName, child, null, session ) ) {
					String parentEntiytName = persister.getEntityName();
					String propertyName = persister.getPropertyNames()[propertyIndex];
					throw new TransientPropertyValueException(
							"object references an unsaved transient instance - save the transient instance before flushing",
							childEntityName,
							parentEntiytName,
							propertyName
					);

				}
			}
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		private boolean isInManagedState(Object child, EventSource session) {
			EntityEntry entry = session.getPersistenceContext().getEntry( child );
			return entry != null &&
					(
							entry.getStatus() == Status.MANAGED ||
									entry.getStatus() == Status.READ_ONLY ||
									entry.getStatus() == Status.SAVING
					);
		}

		@Override
		public String toString() {
			return "ACTION_PERSIST_ON_FLUSH";
		}
	};

	/**
	 * @see org.hibernate.Session#replicate
	 */
	public static final CascadingAction REPLICATE = new BaseCascadingAction() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Object anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to replicate: {0}", entityName );
			session.replicate( entityName, child, (ReplicationMode) anything );
		}

		@Override
		public Iterator getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// replicate does cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false; //I suppose?
		}

		@Override
		public String toString() {
			return "ACTION_REPLICATE";
		}
	};

	public abstract static class BaseCascadingAction implements CascadingAction {
		@Override
		public boolean requiresNoCascadeChecking() {
			return false;
		}

		@Override
		public void noCascade(EventSource session, Object child, Object parent, EntityPersister persister, int propertyIndex) {
		}

		@Override
		public boolean performOnLazyProperty() {
			return true;
		}
	}

	/**
	 * Given a collection, get an iterator of all its children, loading them
	 * from the database if necessary.
	 *
	 * @param session The session within which the cascade is occuring.
	 * @param collectionType The mapping type of the collection.
	 * @param collection The collection instance.
	 *
	 * @return The children iterator.
	 */
	private static Iterator getAllElementsIterator(
			EventSource session,
			CollectionType collectionType,
			Object collection) {
		return collectionType.getElementsIterator( collection, session );
	}

	/**
	 * Iterate just the elements of the collection that are already there. Don't load
	 * any new elements from the database.
	 */
	public static Iterator getLoadedElementsIterator(
			SessionImplementor session,
			CollectionType collectionType,
			Object collection) {
		if ( collectionIsInitialized( collection ) ) {
			// handles arrays and newly instantiated collections
			return collectionType.getElementsIterator( collection, session );
		}
		else {
			// does not handle arrays (thats ok, cos they can't be lazy)
			// or newly instantiated collections, so we can do the cast
			return ((PersistentCollection) collection).queuedAdditionIterator();
		}
	}

	private static boolean collectionIsInitialized(Object collection) {
		return !(collection instanceof PersistentCollection) || ((PersistentCollection) collection).wasInitialized();
	}
}
