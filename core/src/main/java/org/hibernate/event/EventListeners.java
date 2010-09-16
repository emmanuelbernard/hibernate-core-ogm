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
import java.lang.reflect.Field;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.util.Cloneable;

import static org.hibernate.event.EventType.*;

/**
 * A convience holder for all defined session event listeners.
 *
 * @author Steve Ebersole
 *
 * @deprecated Actually it will eventually be retasked for the role it currently plays in the {@link SessionFactory};
 * within the {@link Configuration} event listener collection will be the role of the
 * {@link org.hibernate.event.EventListenerRegistry}
 */
@Deprecated
public class EventListeners extends Cloneable implements Serializable {
	// IMPL NOTE : the delegate approach here (as opposed to an interface + impls) is used to avoid
	// I
	private final EventListenerDelegate delegate;

	public EventListeners(EventListenerDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object shallowCopy() {
		if ( delegate instanceof Cloneable ) {
			return new EventListeners( (EventListenerDelegate) ( (Cloneable) delegate ).shallowCopy() );
		}
		else {
			return new EventListeners( delegate );
		}
	}

	private static interface ListenerProcesser {
		public void processListener(Object listener);
	}

	/**
	 * Call {@link Initializable#initialize} on any listeners that implement the
	 * {@link Initializable} interface.
	 *
	 * @param cfg The configuration.
	 */
	public void initializeListeners(final Configuration cfg) {
		delegate.initializeListeners( cfg );
	}

	/**
	 * Call {@link Destructible#cleanup} on any listeners that implement the
	 * {@link Destructible} interface.
	 */
	public void destroyListeners() {
		delegate.destroyListeners();
	}

	public LoadEventListener[] getLoadEventListeners() {
		return delegate.getLoadEventListeners();
    }

    public void setLoadEventListeners(LoadEventListener[] loadEventListener) {
		delegate.setLoadEventListeners( loadEventListener );
    }

	public ReplicateEventListener[] getReplicateEventListeners() {
		return delegate.getReplicateEventListeners();
	}

	public void setReplicateEventListeners(ReplicateEventListener[] replicateEventListener) {
		delegate.setReplicateEventListeners( replicateEventListener );
	}

	public DeleteEventListener[] getDeleteEventListeners() {
		return delegate.getDeleteEventListeners();
	}

	public void setDeleteEventListeners(DeleteEventListener[] deleteEventListener) {
		delegate.setDeleteEventListeners( deleteEventListener );
	}

	public AutoFlushEventListener[] getAutoFlushEventListeners() {
		return delegate.getAutoFlushEventListeners();
	}

	public void setAutoFlushEventListeners(AutoFlushEventListener[] autoFlushEventListener) {
		delegate.setAutoFlushEventListeners( autoFlushEventListener );
	}

	public DirtyCheckEventListener[] getDirtyCheckEventListeners() {
		return delegate.getDirtyCheckEventListeners();
	}

	public void setDirtyCheckEventListeners(DirtyCheckEventListener[] dirtyCheckEventListener) {
		delegate.setDirtyCheckEventListeners( dirtyCheckEventListener );
	}

	public FlushEventListener[] getFlushEventListeners() {
		return delegate.getFlushEventListeners();
	}

	public void setFlushEventListeners(FlushEventListener[] flushEventListener) {
		delegate.setFlushEventListeners( flushEventListener );
	}

	public EvictEventListener[] getEvictEventListeners() {
		return delegate.getEvictEventListeners();
	}

	public void setEvictEventListeners(EvictEventListener[] evictEventListener) {
		delegate.setEvictEventListeners( evictEventListener );
	}

	public LockEventListener[] getLockEventListeners() {
		return delegate.getLockEventListeners();
	}

	public void setLockEventListeners(LockEventListener[] lockEventListener) {
		delegate.setLockEventListeners( lockEventListener );
	}

	public RefreshEventListener[] getRefreshEventListeners() {
		return delegate.getRefreshEventListeners();
	}

	public void setRefreshEventListeners(RefreshEventListener[] refreshEventListener) {
		delegate.setRefreshEventListeners( refreshEventListener );
	}

	public InitializeCollectionEventListener[] getInitializeCollectionEventListeners() {
		return delegate.getInitializeCollectionEventListeners();
	}

	public void setInitializeCollectionEventListeners(InitializeCollectionEventListener[] initializeCollectionEventListener) {
		delegate.setInitializeCollectionEventListeners( initializeCollectionEventListener );
	}
	
	public FlushEntityEventListener[] getFlushEntityEventListeners() {
		return delegate.getFlushEntityEventListeners();
	}
	
	public void setFlushEntityEventListeners(FlushEntityEventListener[] flushEntityEventListener) {
		delegate.setFlushEntityEventListeners( flushEntityEventListener );
	}
	
	public SaveOrUpdateEventListener[] getSaveOrUpdateEventListeners() {
		return delegate.getSaveOrUpdateEventListeners();
	}
	
	public void setSaveOrUpdateEventListeners(SaveOrUpdateEventListener[] saveOrUpdateEventListener) {
		delegate.setSaveOrUpdateEventListeners( saveOrUpdateEventListener );
	}
	
	public MergeEventListener[] getMergeEventListeners() {
		return delegate.getMergeEventListeners();
	}
	
	public void setMergeEventListeners(MergeEventListener[] mergeEventListener) {
		delegate.setMergeEventListeners( mergeEventListener );
	}
	
	public PersistEventListener[] getPersistEventListeners() {
		return delegate.getPersistEventListeners();
	}
	
	public void setPersistEventListeners(PersistEventListener[] createEventListener) {
		delegate.setPersistEventListeners( createEventListener );
	}

	public PersistEventListener[] getPersistOnFlushEventListeners() {
		return delegate.getPersistOnFlushEventListeners();
	}

	public void setPersistOnFlushEventListeners(PersistEventListener[] createEventListener) {
		delegate.setPersistOnFlushEventListeners( createEventListener );
	}
	
	public MergeEventListener[] getSaveOrUpdateCopyEventListeners() {
		return delegate.getSaveOrUpdateCopyEventListeners();
	}
	
	public void setSaveOrUpdateCopyEventListeners(MergeEventListener[] saveOrUpdateCopyEventListener) {
		delegate.setSaveOrUpdateCopyEventListeners( saveOrUpdateCopyEventListener );
	}
	
	public SaveOrUpdateEventListener[] getSaveEventListeners() {
		return delegate.getSaveEventListeners();
	}
	
	public void setSaveEventListeners(SaveOrUpdateEventListener[] saveEventListener) {
		delegate.setSaveEventListeners( saveEventListener );
	}
	
	public SaveOrUpdateEventListener[] getUpdateEventListeners() {
		return delegate.getUpdateEventListeners();
	}
	
	public void setUpdateEventListeners(SaveOrUpdateEventListener[] updateEventListener) {
		delegate.setUpdateEventListeners( updateEventListener );
	}

	public PostLoadEventListener[] getPostLoadEventListeners() {
		return delegate.getPostLoadEventListeners();
	}

	public void setPostLoadEventListeners(PostLoadEventListener[] postLoadEventListener) {
		delegate.setPostLoadEventListeners( postLoadEventListener );
	}

	public PreLoadEventListener[] getPreLoadEventListeners() {
		return delegate.getPreLoadEventListeners();
	}

	public void setPreLoadEventListeners(PreLoadEventListener[] preLoadEventListener) {
		delegate.setPreLoadEventListeners( preLoadEventListener );
	}

	public PreCollectionRecreateEventListener[] getPreCollectionRecreateEventListeners() {
		return delegate.getPreCollectionRecreateEventListeners();
	}

	public void setPreCollectionRecreateEventListeners(PreCollectionRecreateEventListener[] preCollectionRecreateEventListener) {
		delegate.setPreCollectionRecreateEventListeners( preCollectionRecreateEventListener );
	}

	public PreCollectionRemoveEventListener[] getPreCollectionRemoveEventListeners() {
		return delegate.getPreCollectionRemoveEventListeners();
	}

	public void setPreCollectionRemoveEventListeners(PreCollectionRemoveEventListener[] preCollectionRemoveEventListener) {
		delegate.setPreCollectionRemoveEventListeners( preCollectionRemoveEventListener );
	}

	public PreCollectionUpdateEventListener[] getPreCollectionUpdateEventListeners() {
		return delegate.getPreCollectionUpdateEventListeners();
	}

	public void setPreCollectionUpdateEventListeners(PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners) {
		delegate.setPreCollectionUpdateEventListeners( preCollectionUpdateEventListeners );
	}

	public PostDeleteEventListener[] getPostDeleteEventListeners() {
		return delegate.getPostDeleteEventListeners();
	}

	public void setPostDeleteEventListeners(PostDeleteEventListener[] postDeleteEventListener) {
		delegate.setPostDeleteEventListeners( postDeleteEventListener );
	}
	
	public PostInsertEventListener[] getPostInsertEventListeners() {
		return delegate.getPostInsertEventListeners();
	}

	public void setPostInsertEventListeners(PostInsertEventListener[] postInsertEventListener) {
		delegate.setPostInsertEventListeners( postInsertEventListener );
	}
	
	public PostUpdateEventListener[] getPostUpdateEventListeners() {
		return delegate.getPostUpdateEventListeners();
	}
	
	public void setPostUpdateEventListeners(PostUpdateEventListener[] postUpdateEventListener) {
		delegate.setPostUpdateEventListeners( postUpdateEventListener );
	}

	public PostCollectionRecreateEventListener[] getPostCollectionRecreateEventListeners() {
		return delegate.getPostCollectionRecreateEventListeners();
	}

	public void setPostCollectionRecreateEventListeners(PostCollectionRecreateEventListener[] postCollectionRecreateEventListener) {
		delegate.setPostCollectionRecreateEventListeners( postCollectionRecreateEventListener );
	}

	public PostCollectionRemoveEventListener[] getPostCollectionRemoveEventListeners() {
		return delegate.getPostCollectionRemoveEventListeners();
	}

	public void setPostCollectionRemoveEventListeners(PostCollectionRemoveEventListener[] postCollectionRemoveEventListener) {
		delegate.setPostCollectionRemoveEventListeners( postCollectionRemoveEventListener );
	}	        

	public PostCollectionUpdateEventListener[] getPostCollectionUpdateEventListeners() {
		return delegate.getPostCollectionUpdateEventListeners();
	}

	public void setPostCollectionUpdateEventListeners(PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners) {
		delegate.setPostCollectionUpdateEventListeners( postCollectionUpdateEventListeners );
	}

	public PreDeleteEventListener[] getPreDeleteEventListeners() {
		return delegate.getPreDeleteEventListeners();
	}
	
	public void setPreDeleteEventListeners(PreDeleteEventListener[] preDeleteEventListener) {
		delegate.setPreDeleteEventListeners( preDeleteEventListener );
	}
	
	public PreInsertEventListener[] getPreInsertEventListeners() {
		return delegate.getPreInsertEventListeners();
	}
	
	public void setPreInsertEventListeners(PreInsertEventListener[] preInsertEventListener) {
		delegate.setPreInsertEventListeners( preInsertEventListener );
	}
	
	public PreUpdateEventListener[] getPreUpdateEventListeners() {
		return delegate.getPreUpdateEventListeners();
	}
	
	public void setPreUpdateEventListeners(PreUpdateEventListener[] preUpdateEventListener) {
		delegate.setPreUpdateEventListeners( preUpdateEventListener );
	}

	public PostDeleteEventListener[] getPostCommitDeleteEventListeners() {
		return delegate.getPostCommitDeleteEventListeners();
	}

	public void setPostCommitDeleteEventListeners(PostDeleteEventListener[] postCommitDeleteEventListeners) {
		delegate.setPostCommitDeleteEventListeners( postCommitDeleteEventListeners );
	}

	public PostInsertEventListener[] getPostCommitInsertEventListeners() {
		return delegate.getPostCommitInsertEventListeners();
	}

	public void setPostCommitInsertEventListeners(PostInsertEventListener[] postCommitInsertEventListeners) {
		delegate.setPostCommitInsertEventListeners( postCommitInsertEventListeners );
	}

	public PostUpdateEventListener[] getPostCommitUpdateEventListeners() {
		return delegate.getPostCommitUpdateEventListeners();
	}

	public void setPostCommitUpdateEventListeners(PostUpdateEventListener[] postCommitUpdateEventListeners) {
		delegate.setPostCommitUpdateEventListeners( postCommitUpdateEventListeners );
	}

	/**
	 * Internally how do we delegate the method calls.  Again, this is just to support backwards compatibility for
	 * a short time.
	 */
	public static interface EventListenerDelegate {
		public void initializeListeners(Configuration configuration);
		public void destroyListeners();

		public LoadEventListener[] getLoadEventListeners();

		public void setLoadEventListeners(LoadEventListener[] loadEventListener);

		public ReplicateEventListener[] getReplicateEventListeners();

		public void setReplicateEventListeners(ReplicateEventListener[] replicateEventListener);

		public DeleteEventListener[] getDeleteEventListeners();

		public void setDeleteEventListeners(DeleteEventListener[] deleteEventListener);

		public AutoFlushEventListener[] getAutoFlushEventListeners();

		public void setAutoFlushEventListeners(AutoFlushEventListener[] autoFlushEventListener);

		public DirtyCheckEventListener[] getDirtyCheckEventListeners();

		public void setDirtyCheckEventListeners(DirtyCheckEventListener[] dirtyCheckEventListener);

		public FlushEventListener[] getFlushEventListeners();

		public void setFlushEventListeners(FlushEventListener[] flushEventListener);

		public EvictEventListener[] getEvictEventListeners();

		public void setEvictEventListeners(EvictEventListener[] evictEventListener);

		public LockEventListener[] getLockEventListeners();

		public void setLockEventListeners(LockEventListener[] lockEventListener);

		public RefreshEventListener[] getRefreshEventListeners();

		public void setRefreshEventListeners(RefreshEventListener[] refreshEventListener);

		public InitializeCollectionEventListener[] getInitializeCollectionEventListeners();

		public void setInitializeCollectionEventListeners(InitializeCollectionEventListener[] initializeCollectionEventListener);

		public FlushEntityEventListener[] getFlushEntityEventListeners();

		public void setFlushEntityEventListeners(FlushEntityEventListener[] flushEntityEventListener);

		public SaveOrUpdateEventListener[] getSaveOrUpdateEventListeners();

		public void setSaveOrUpdateEventListeners(SaveOrUpdateEventListener[] saveOrUpdateEventListener);

		public MergeEventListener[] getMergeEventListeners();

		public void setMergeEventListeners(MergeEventListener[] mergeEventListener);

		public PersistEventListener[] getPersistEventListeners();

		public void setPersistEventListeners(PersistEventListener[] createEventListener);

		public PersistEventListener[] getPersistOnFlushEventListeners();

		public void setPersistOnFlushEventListeners(PersistEventListener[] createEventListener);

		public MergeEventListener[] getSaveOrUpdateCopyEventListeners();

		public void setSaveOrUpdateCopyEventListeners(MergeEventListener[] saveOrUpdateCopyEventListener);

		public SaveOrUpdateEventListener[] getSaveEventListeners();

		public void setSaveEventListeners(SaveOrUpdateEventListener[] saveEventListener);

		public SaveOrUpdateEventListener[] getUpdateEventListeners();

		public void setUpdateEventListeners(SaveOrUpdateEventListener[] updateEventListener);

		public PostLoadEventListener[] getPostLoadEventListeners();

		public void setPostLoadEventListeners(PostLoadEventListener[] postLoadEventListener);

		public PreLoadEventListener[] getPreLoadEventListeners();

		public void setPreLoadEventListeners(PreLoadEventListener[] preLoadEventListener);

		public PreCollectionRecreateEventListener[] getPreCollectionRecreateEventListeners();

		public void setPreCollectionRecreateEventListeners(PreCollectionRecreateEventListener[] preCollectionRecreateEventListener);

		public PreCollectionRemoveEventListener[] getPreCollectionRemoveEventListeners();

		public void setPreCollectionRemoveEventListeners(PreCollectionRemoveEventListener[] preCollectionRemoveEventListener);

		public PreCollectionUpdateEventListener[] getPreCollectionUpdateEventListeners();

		public void setPreCollectionUpdateEventListeners(PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners);

		public PostDeleteEventListener[] getPostDeleteEventListeners();

		public PostInsertEventListener[] getPostInsertEventListeners();

		public PostUpdateEventListener[] getPostUpdateEventListeners();

		public void setPostDeleteEventListeners(PostDeleteEventListener[] postDeleteEventListener);

		public void setPostInsertEventListeners(PostInsertEventListener[] postInsertEventListener);

		public void setPostUpdateEventListeners(PostUpdateEventListener[] postUpdateEventListener);

		public PostCollectionRecreateEventListener[] getPostCollectionRecreateEventListeners();

		public void setPostCollectionRecreateEventListeners(PostCollectionRecreateEventListener[] postCollectionRecreateEventListener);

		public PostCollectionRemoveEventListener[] getPostCollectionRemoveEventListeners();

		public void setPostCollectionRemoveEventListeners(PostCollectionRemoveEventListener[] postCollectionRemoveEventListener);

		public PostCollectionUpdateEventListener[] getPostCollectionUpdateEventListeners();

		public void setPostCollectionUpdateEventListeners(PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners);

		public PreDeleteEventListener[] getPreDeleteEventListeners();

		public void setPreDeleteEventListeners(PreDeleteEventListener[] preDeleteEventListener);

		public PreInsertEventListener[] getPreInsertEventListeners();

		public void setPreInsertEventListeners(PreInsertEventListener[] preInsertEventListener);

		public PreUpdateEventListener[] getPreUpdateEventListeners();

		public void setPreUpdateEventListeners(PreUpdateEventListener[] preUpdateEventListener);

		public PostDeleteEventListener[] getPostCommitDeleteEventListeners();

		public void setPostCommitDeleteEventListeners(PostDeleteEventListener[] postCommitDeleteEventListeners);

		public PostInsertEventListener[] getPostCommitInsertEventListeners();

		public void setPostCommitInsertEventListeners(PostInsertEventListener[] postCommitInsertEventListeners);

		public PostUpdateEventListener[] getPostCommitUpdateEventListeners();

		public void setPostCommitUpdateEventListeners(PostUpdateEventListener[] postCommitUpdateEventListeners);
	}

	@SuppressWarnings({ "unchecked" })
	public static class ConfigurationDelegate implements EventListenerDelegate {
		private final Configuration configuration;

		public ConfigurationDelegate(Configuration configuration) {
			this.configuration = configuration;
		}

		public void initializeListeners(Configuration configuration) {
			// noop
		}

		public void destroyListeners() {
			// noop
		}

		private EventListenerRegistry registry() {
			return configuration.getEventListenerRegistry();
		}

		public LoadEventListener[] getLoadEventListeners() {
			return (LoadEventListener[]) registry().getRegisteredEventListeners( LOAD ).getListenerArray();
		}

		public void setLoadEventListeners(LoadEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( LOAD ), listeners );
		}

		private void setListeners(RegisteredEventListeners registeredListeners, Object[] listeners) {
			registeredListeners.clear();
			for ( Object listener : listeners ) {
				registeredListeners.appendListener( listener );
			}
		}

		public ReplicateEventListener[] getReplicateEventListeners() {
			return (ReplicateEventListener[]) registry().getRegisteredEventListeners( REPLICATE ).getListenerArray();
		}

		public void setReplicateEventListeners(ReplicateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( REPLICATE ), listeners );
		}

		public DeleteEventListener[] getDeleteEventListeners() {
			return (DeleteEventListener[]) registry().getRegisteredEventListeners( DELETE ).getListenerArray();
		}

		public void setDeleteEventListeners(DeleteEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( DELETE ), listeners );
		}

		public AutoFlushEventListener[] getAutoFlushEventListeners() {
			return (AutoFlushEventListener[]) registry().getRegisteredEventListeners( AUTO_FLUSH ).getListenerArray();
		}

		public void setAutoFlushEventListeners(AutoFlushEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( AUTO_FLUSH ), listeners );
		}

		public DirtyCheckEventListener[] getDirtyCheckEventListeners() {
			return (DirtyCheckEventListener[]) registry().getRegisteredEventListeners( DIRTY_CHECK ).getListenerArray();
		}

		public void setDirtyCheckEventListeners(DirtyCheckEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( DIRTY_CHECK ), listeners );
		}

		public FlushEventListener[] getFlushEventListeners() {
			return (FlushEventListener[]) registry().getRegisteredEventListeners( FLUSH ).getListenerArray();
		}

		public void setFlushEventListeners(FlushEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( FLUSH ), listeners );
		}

		public EvictEventListener[] getEvictEventListeners() {
			return (EvictEventListener[]) registry().getRegisteredEventListeners( EVICT ).getListenerArray();
		}

		public void setEvictEventListeners(EvictEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( EVICT ), listeners );
		}

		public LockEventListener[] getLockEventListeners() {
			return (LockEventListener[]) registry().getRegisteredEventListeners( LOCK ).getListenerArray();
		}

		public void setLockEventListeners(LockEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( LOCK ), listeners );
		}

		public RefreshEventListener[] getRefreshEventListeners() {
			return (RefreshEventListener[]) registry().getRegisteredEventListeners( REFRESH ).getListenerArray();
		}

		public void setRefreshEventListeners(RefreshEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( REFRESH ), listeners );
		}

		public InitializeCollectionEventListener[] getInitializeCollectionEventListeners() {
			return (InitializeCollectionEventListener[]) registry().getRegisteredEventListeners( INIT_COLLECTION ).getListenerArray();
		}

		public void setInitializeCollectionEventListeners(InitializeCollectionEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( INIT_COLLECTION ), listeners );
		}

		public FlushEntityEventListener[] getFlushEntityEventListeners() {
			return (FlushEntityEventListener[]) registry().getRegisteredEventListeners( FLUSH_ENTITY ).getListenerArray();
		}

		public void setFlushEntityEventListeners(FlushEntityEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( FLUSH_ENTITY ), listeners );
		}

		public SaveOrUpdateEventListener[] getSaveOrUpdateEventListeners() {
			return (SaveOrUpdateEventListener[]) registry().getRegisteredEventListeners( SAVE_UPDATE ).getListenerArray();
		}

		public void setSaveOrUpdateEventListeners(SaveOrUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( SAVE_UPDATE ), listeners );
		}

		public MergeEventListener[] getMergeEventListeners() {
			return (MergeEventListener[]) registry().getRegisteredEventListeners( MERGE ).getListenerArray();
		}

		public void setMergeEventListeners(MergeEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( MERGE ), listeners );
		}

		public PersistEventListener[] getPersistEventListeners() {
			return (PersistEventListener[]) registry().getRegisteredEventListeners( PERSIST ).getListenerArray();
		}

		public void setPersistEventListeners(PersistEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PERSIST ), listeners );
		}

		public PersistEventListener[] getPersistOnFlushEventListeners() {
			return (PersistEventListener[]) registry().getRegisteredEventListeners( PERSIST_ONFLUSH ).getListenerArray();
		}

		public void setPersistOnFlushEventListeners(PersistEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PERSIST_ONFLUSH ), listeners );
		}

		public MergeEventListener[] getSaveOrUpdateCopyEventListeners() {
			return (MergeEventListener[]) registry().getRegisteredEventListeners( SAVE_UPDATE_COPY ).getListenerArray();
		}

		public void setSaveOrUpdateCopyEventListeners(MergeEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( SAVE_UPDATE_COPY ), listeners );
		}

		public SaveOrUpdateEventListener[] getSaveEventListeners() {
			return (SaveOrUpdateEventListener[]) registry().getRegisteredEventListeners( SAVE ).getListenerArray();
		}

		public void setSaveEventListeners(SaveOrUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( SAVE ), listeners );
		}

		public SaveOrUpdateEventListener[] getUpdateEventListeners() {
			return (SaveOrUpdateEventListener[]) registry().getRegisteredEventListeners( UPDATE ).getListenerArray();
		}

		public void setUpdateEventListeners(SaveOrUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( UPDATE ), listeners );
		}

		public PostLoadEventListener[] getPostLoadEventListeners() {
			return (PostLoadEventListener[]) registry().getRegisteredEventListeners( POST_LOAD ).getListenerArray();
		}

		public void setPostLoadEventListeners(PostLoadEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_LOAD ), listeners );
		}

		public PreLoadEventListener[] getPreLoadEventListeners() {
			return (PreLoadEventListener[]) registry().getRegisteredEventListeners( PRE_LOAD ).getListenerArray();
		}

		public void setPreLoadEventListeners(PreLoadEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_LOAD ), listeners );
		}

		public PreCollectionRecreateEventListener[] getPreCollectionRecreateEventListeners() {
			return (PreCollectionRecreateEventListener[]) registry().getRegisteredEventListeners( PRE_COLLECTION_RECREATE ).getListenerArray();
		}

		public void setPreCollectionRecreateEventListeners(PreCollectionRecreateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_COLLECTION_RECREATE ), listeners );
		}

		public PreCollectionRemoveEventListener[] getPreCollectionRemoveEventListeners() {
			return (PreCollectionRemoveEventListener[]) registry().getRegisteredEventListeners( PRE_COLLECTION_REMOVE ).getListenerArray();
		}

		public void setPreCollectionRemoveEventListeners(PreCollectionRemoveEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_COLLECTION_REMOVE ), listeners );
		}

		public PreCollectionUpdateEventListener[] getPreCollectionUpdateEventListeners() {
			return (PreCollectionUpdateEventListener[]) registry().getRegisteredEventListeners( PRE_COLLECTION_UPDATE ).getListenerArray();
		}

		public void setPreCollectionUpdateEventListeners(PreCollectionUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_COLLECTION_UPDATE ), listeners );
		}

		public PostDeleteEventListener[] getPostDeleteEventListeners() {
			return (PostDeleteEventListener[]) registry().getRegisteredEventListeners( POST_DELETE ).getListenerArray();
		}

		public PostInsertEventListener[] getPostInsertEventListeners() {
			return (PostInsertEventListener[]) registry().getRegisteredEventListeners( POST_INSERT ).getListenerArray();
		}

		public PostUpdateEventListener[] getPostUpdateEventListeners() {
			return (PostUpdateEventListener[]) registry().getRegisteredEventListeners( POST_UPDATE ).getListenerArray();
		}

		public void setPostDeleteEventListeners(PostDeleteEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_DELETE ), listeners );
		}

		public void setPostInsertEventListeners(PostInsertEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_INSERT ), listeners );
		}

		public void setPostUpdateEventListeners(PostUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_UPDATE ), listeners );
		}

		public PostCollectionRecreateEventListener[] getPostCollectionRecreateEventListeners() {
			return (PostCollectionRecreateEventListener[]) registry().getRegisteredEventListeners( POST_COLLECTION_RECREATE ).getListenerArray();
		}

		public void setPostCollectionRecreateEventListeners(PostCollectionRecreateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_COLLECTION_RECREATE ), listeners );
		}

		public PostCollectionRemoveEventListener[] getPostCollectionRemoveEventListeners() {
			return (PostCollectionRemoveEventListener[]) registry().getRegisteredEventListeners( POST_COLLECTION_REMOVE ).getListenerArray();
		}

		public void setPostCollectionRemoveEventListeners(PostCollectionRemoveEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_COLLECTION_REMOVE ), listeners );
		}

		public PostCollectionUpdateEventListener[] getPostCollectionUpdateEventListeners() {
			return (PostCollectionUpdateEventListener[]) registry().getRegisteredEventListeners( POST_COLLECTION_UPDATE ).getListenerArray();
		}

		public void setPostCollectionUpdateEventListeners(PostCollectionUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_COLLECTION_UPDATE ), listeners );
		}

		public PreDeleteEventListener[] getPreDeleteEventListeners() {
			return (PreDeleteEventListener[]) registry().getRegisteredEventListeners( PRE_DELETE ).getListenerArray();
		}

		public void setPreDeleteEventListeners(PreDeleteEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_DELETE ), listeners );
		}

		public PreInsertEventListener[] getPreInsertEventListeners() {
			return (PreInsertEventListener[]) registry().getRegisteredEventListeners( PRE_INSERT ).getListenerArray();
		}

		public void setPreInsertEventListeners(PreInsertEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_INSERT ), listeners );
		}

		public PreUpdateEventListener[] getPreUpdateEventListeners() {
			return (PreUpdateEventListener[]) registry().getRegisteredEventListeners( PRE_UPDATE ).getListenerArray();
		}

		public void setPreUpdateEventListeners(PreUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( PRE_UPDATE ), listeners );
		}

		public PostDeleteEventListener[] getPostCommitDeleteEventListeners() {
			return (PostDeleteEventListener[]) registry().getRegisteredEventListeners( POST_COMMIT_DELETE ).getListenerArray();
		}

		public void setPostCommitDeleteEventListeners(PostDeleteEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_COMMIT_DELETE ), listeners );
		}

		public PostInsertEventListener[] getPostCommitInsertEventListeners() {
			return (PostInsertEventListener[]) registry().getRegisteredEventListeners( POST_COMMIT_INSERT).getListenerArray();
		}

		public void setPostCommitInsertEventListeners(PostInsertEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_COMMIT_INSERT ), listeners );
		}

		public PostUpdateEventListener[] getPostCommitUpdateEventListeners() {
			return (PostUpdateEventListener[]) registry().getRegisteredEventListeners( POST_COMMIT_UPDATE).getListenerArray();
		}

		public void setPostCommitUpdateEventListeners(PostUpdateEventListener[] listeners) {
			setListeners( registry().getRegisteredEventListeners( POST_COMMIT_UPDATE ), listeners );
		}
	}

	public static class SessionFactoryDelegate extends Cloneable implements EventListenerDelegate {
		private final LoadEventListener[] loadEventListeners;
		private final SaveOrUpdateEventListener[] saveOrUpdateEventListeners;
		private final MergeEventListener[] mergeEventListeners;
		private final PersistEventListener[] persistEventListeners;
		private final PersistEventListener[] persistOnFlushEventListeners;
		private final ReplicateEventListener[] replicateEventListeners;
		private final DeleteEventListener[] deleteEventListeners;
		private final AutoFlushEventListener[] autoFlushEventListeners;
		private final DirtyCheckEventListener[] dirtyCheckEventListeners;
		private final FlushEventListener[] flushEventListeners;
		private final EvictEventListener[] evictEventListeners;
		private final LockEventListener[] lockEventListeners;
		private final RefreshEventListener[] refreshEventListeners;
		private final FlushEntityEventListener[] flushEntityEventListeners;
		private final InitializeCollectionEventListener[] initializeCollectionEventListeners;
		private final PostLoadEventListener[] postLoadEventListeners;
		private final PreLoadEventListener[] preLoadEventListeners;
		private final PreDeleteEventListener[] preDeleteEventListeners;
		private final PreUpdateEventListener[] preUpdateEventListeners;
		private final PreInsertEventListener[] preInsertEventListeners;
		private final PostDeleteEventListener[] postDeleteEventListeners;
		private final PostUpdateEventListener[] postUpdateEventListeners;
		private final PostInsertEventListener[] postInsertEventListeners;
		private final PostDeleteEventListener[] postCommitDeleteEventListeners;
		private final PostUpdateEventListener[] postCommitUpdateEventListeners;
		private final PostInsertEventListener[] postCommitInsertEventListeners;
		private final PreCollectionRecreateEventListener[] preCollectionRecreateEventListeners;
		private final PostCollectionRecreateEventListener[] postCollectionRecreateEventListeners;
		private final PreCollectionRemoveEventListener[] preCollectionRemoveEventListeners;
		private final PostCollectionRemoveEventListener[] postCollectionRemoveEventListeners;
		private final PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners;
		private final PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners;
		private final SaveOrUpdateEventListener[] saveEventListeners;
		private final SaveOrUpdateEventListener[] updateEventListeners;
		private final MergeEventListener[] saveOrUpdateCopyEventListeners;

		public SessionFactoryDelegate(
				LoadEventListener[] loadEventListeners,
				SaveOrUpdateEventListener[] saveOrUpdateEventListeners,
				MergeEventListener[] mergeEventListeners,
				PersistEventListener[] persistEventListeners,
				PersistEventListener[] persistOnFlushEventListeners,
				ReplicateEventListener[] replicateEventListeners,
				DeleteEventListener[] deleteEventListeners,
				AutoFlushEventListener[] autoFlushEventListeners,
				DirtyCheckEventListener[] dirtyCheckEventListeners,
				FlushEventListener[] flushEventListeners,
				EvictEventListener[] evictEventListeners,
				LockEventListener[] lockEventListeners,
				RefreshEventListener[] refreshEventListeners,
				FlushEntityEventListener[] flushEntityEventListeners,
				InitializeCollectionEventListener[] initializeCollectionEventListeners,
				PostLoadEventListener[] postLoadEventListeners,
				PreLoadEventListener[] preLoadEventListeners,
				PreDeleteEventListener[] preDeleteEventListeners,
				PreUpdateEventListener[] preUpdateEventListeners,
				PreInsertEventListener[] preInsertEventListeners,
				PostDeleteEventListener[] postDeleteEventListeners,
				PostUpdateEventListener[] postUpdateEventListeners,
				PostInsertEventListener[] postInsertEventListeners,
				PostDeleteEventListener[] postCommitDeleteEventListeners,
				PostUpdateEventListener[] postCommitUpdateEventListeners,
				PostInsertEventListener[] postCommitInsertEventListeners,
				PreCollectionRecreateEventListener[] preCollectionRecreateEventListeners,
				PostCollectionRecreateEventListener[] postCollectionRecreateEventListeners,
				PreCollectionRemoveEventListener[] preCollectionRemoveEventListeners,
				PostCollectionRemoveEventListener[] postCollectionRemoveEventListeners,
				PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners,
				PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners,
				SaveOrUpdateEventListener[] saveEventListeners,
				SaveOrUpdateEventListener[] updateEventListeners,
				MergeEventListener[] saveOrUpdateCopyEventListeners) {
			this.loadEventListeners = loadEventListeners;
			this.saveOrUpdateEventListeners = saveOrUpdateEventListeners;
			this.mergeEventListeners = mergeEventListeners;
			this.persistEventListeners = persistEventListeners;
			this.persistOnFlushEventListeners = persistOnFlushEventListeners;
			this.replicateEventListeners = replicateEventListeners;
			this.deleteEventListeners = deleteEventListeners;
			this.autoFlushEventListeners = autoFlushEventListeners;
			this.dirtyCheckEventListeners = dirtyCheckEventListeners;
			this.flushEventListeners = flushEventListeners;
			this.evictEventListeners = evictEventListeners;
			this.lockEventListeners = lockEventListeners;
			this.refreshEventListeners = refreshEventListeners;
			this.flushEntityEventListeners = flushEntityEventListeners;
			this.initializeCollectionEventListeners = initializeCollectionEventListeners;
			this.postLoadEventListeners = postLoadEventListeners;
			this.preLoadEventListeners = preLoadEventListeners;
			this.preDeleteEventListeners = preDeleteEventListeners;
			this.preUpdateEventListeners = preUpdateEventListeners;
			this.preInsertEventListeners = preInsertEventListeners;
			this.postDeleteEventListeners = postDeleteEventListeners;
			this.postUpdateEventListeners = postUpdateEventListeners;
			this.postInsertEventListeners = postInsertEventListeners;
			this.postCommitDeleteEventListeners = postCommitDeleteEventListeners;
			this.postCommitUpdateEventListeners = postCommitUpdateEventListeners;
			this.postCommitInsertEventListeners = postCommitInsertEventListeners;
			this.preCollectionRecreateEventListeners = preCollectionRecreateEventListeners;
			this.postCollectionRecreateEventListeners = postCollectionRecreateEventListeners;
			this.preCollectionRemoveEventListeners = preCollectionRemoveEventListeners;
			this.postCollectionRemoveEventListeners = postCollectionRemoveEventListeners;
			this.preCollectionUpdateEventListeners = preCollectionUpdateEventListeners;
			this.postCollectionUpdateEventListeners = postCollectionUpdateEventListeners;
			this.saveEventListeners = saveEventListeners;
			this.updateEventListeners = updateEventListeners;
			this.saveOrUpdateCopyEventListeners = saveOrUpdateCopyEventListeners;
		}

		private void processListeners(ListenerProcesser processer) {
			Field[] fields = getClass().getDeclaredFields();
			for ( int i = 0; i < fields.length; i++ ) {
				final Object[] listeners;
				try {
					Object fieldValue = fields[i].get(this);
					if ( fieldValue instanceof Object[] ) {
						listeners = ( Object[] ) fieldValue;
					}
					else {
						continue;
					}
				}
				catch ( Throwable t ) {
					throw new HibernateException( "could not init listeners", t );
				}

				int length = listeners.length;
				for ( int index = 0 ; index < length ; index++ ) {
					processer.processListener( listeners[index ] );
				}
			}
		}

		public void initializeListeners(final Configuration cfg) {
			try {
				processListeners(
						new ListenerProcesser() {
							public void processListener(Object listener) {
								if ( listener instanceof Initializable ) {
									( ( Initializable ) listener ).initialize( cfg );
								}
							}
						}
				);
			}
			catch ( Exception e ) {
				throw new HibernateException("could not init listeners", e);
			}
		}

		public void destroyListeners() {
			try {
				processListeners(
						new ListenerProcesser() {
							public void processListener(Object listener) {
								if ( listener instanceof Destructible ) {
									( ( Destructible ) listener ).cleanup();
								}
							}
						}
				);
			}
			catch ( Exception e ) {
				throw new HibernateException("could not destruct listeners", e);
			}
		}

		public final LoadEventListener[] getLoadEventListeners() {
			return loadEventListeners;
		}

		public void setLoadEventListeners(LoadEventListener[] loadEventListener) {
			throw immutableError();
		}

		private EventListenerRegistrationException immutableError() {
			return new EventListenerRegistrationException( "Cannot alter listers after SessionFactory built" );
		}

		public ReplicateEventListener[] getReplicateEventListeners() {
			return replicateEventListeners;
		}

		public void setReplicateEventListeners(ReplicateEventListener[] replicateEventListener) {
			throw immutableError();
		}

		public DeleteEventListener[] getDeleteEventListeners() {
			return deleteEventListeners;
		}

		public void setDeleteEventListeners(DeleteEventListener[] deleteEventListener) {
			throw immutableError();
		}

		public AutoFlushEventListener[] getAutoFlushEventListeners() {
			return autoFlushEventListeners;
		}

		public void setAutoFlushEventListeners(AutoFlushEventListener[] autoFlushEventListener) {
			throw immutableError();
		}

		public DirtyCheckEventListener[] getDirtyCheckEventListeners() {
			return dirtyCheckEventListeners;
		}

		public void setDirtyCheckEventListeners(DirtyCheckEventListener[] dirtyCheckEventListener) {
			throw immutableError();
		}

		public FlushEventListener[] getFlushEventListeners() {
			return flushEventListeners;
		}

		public void setFlushEventListeners(FlushEventListener[] flushEventListener) {
			throw immutableError();
		}

		public EvictEventListener[] getEvictEventListeners() {
			return evictEventListeners;
		}

		public void setEvictEventListeners(EvictEventListener[] evictEventListener) {
			throw immutableError();
		}

		public LockEventListener[] getLockEventListeners() {
			return lockEventListeners;
		}

		public void setLockEventListeners(LockEventListener[] lockEventListener) {
			throw immutableError();
		}

		public RefreshEventListener[] getRefreshEventListeners() {
			return refreshEventListeners;
		}

		public void setRefreshEventListeners(RefreshEventListener[] refreshEventListener) {
			throw immutableError();
		}

		public InitializeCollectionEventListener[] getInitializeCollectionEventListeners() {
			return initializeCollectionEventListeners;
		}

		public void setInitializeCollectionEventListeners(InitializeCollectionEventListener[] initializeCollectionEventListener) {
			throw immutableError();
		}

		public FlushEntityEventListener[] getFlushEntityEventListeners() {
			return flushEntityEventListeners;
		}

		public void setFlushEntityEventListeners(FlushEntityEventListener[] flushEntityEventListener) {
			throw immutableError();
		}

		public SaveOrUpdateEventListener[] getSaveOrUpdateEventListeners() {
			return saveOrUpdateEventListeners;
		}

		public void setSaveOrUpdateEventListeners(SaveOrUpdateEventListener[] saveOrUpdateEventListener) {
			throw immutableError();
		}

		public MergeEventListener[] getMergeEventListeners() {
			return mergeEventListeners;
		}

		public void setMergeEventListeners(MergeEventListener[] mergeEventListener) {
			throw immutableError();
		}

		public PersistEventListener[] getPersistEventListeners() {
			return persistEventListeners;
		}

		public void setPersistEventListeners(PersistEventListener[] createEventListener) {
			throw immutableError();
		}

		public PersistEventListener[] getPersistOnFlushEventListeners() {
			return persistOnFlushEventListeners;
		}

		public void setPersistOnFlushEventListeners(PersistEventListener[] createEventListener) {
			throw immutableError();
		}

		public MergeEventListener[] getSaveOrUpdateCopyEventListeners() {
			return saveOrUpdateCopyEventListeners;
		}

		public void setSaveOrUpdateCopyEventListeners(MergeEventListener[] saveOrUpdateCopyEventListener) {
			throw immutableError();
		}

		public SaveOrUpdateEventListener[] getSaveEventListeners() {
			return saveEventListeners;
		}

		public void setSaveEventListeners(SaveOrUpdateEventListener[] saveEventListener) {
			throw immutableError();
		}

		public SaveOrUpdateEventListener[] getUpdateEventListeners() {
			return updateEventListeners;
		}

		public void setUpdateEventListeners(SaveOrUpdateEventListener[] updateEventListener) {
			throw immutableError();
		}

		public PostLoadEventListener[] getPostLoadEventListeners() {
			return postLoadEventListeners;
		}

		public void setPostLoadEventListeners(PostLoadEventListener[] postLoadEventListener) {
			throw immutableError();
		}

		public PreLoadEventListener[] getPreLoadEventListeners() {
			return preLoadEventListeners;
		}

		public void setPreLoadEventListeners(PreLoadEventListener[] preLoadEventListener) {
			throw immutableError();
		}

		public PreCollectionRecreateEventListener[] getPreCollectionRecreateEventListeners() {
			return preCollectionRecreateEventListeners;
		}

		public void setPreCollectionRecreateEventListeners(PreCollectionRecreateEventListener[] preCollectionRecreateEventListener) {
			throw immutableError();
		}

		public PreCollectionRemoveEventListener[] getPreCollectionRemoveEventListeners() {
			return preCollectionRemoveEventListeners;
		}

		public void setPreCollectionRemoveEventListeners(PreCollectionRemoveEventListener[] preCollectionRemoveEventListener) {
			throw immutableError();
		}

		public PreCollectionUpdateEventListener[] getPreCollectionUpdateEventListeners() {
			return preCollectionUpdateEventListeners;
		}

		public void setPreCollectionUpdateEventListeners(PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners) {
			throw immutableError();
		}

		public PostDeleteEventListener[] getPostDeleteEventListeners() {
			return postDeleteEventListeners;
		}

		public void setPostDeleteEventListeners(PostDeleteEventListener[] postDeleteEventListener) {
			throw immutableError();
		}

		public PostInsertEventListener[] getPostInsertEventListeners() {
			return postInsertEventListeners;
		}

		public void setPostUpdateEventListeners(PostUpdateEventListener[] postUpdateEventListener) {
			throw immutableError();
		}

		public PostUpdateEventListener[] getPostUpdateEventListeners() {
			return postUpdateEventListeners;
		}

		public void setPostInsertEventListeners(PostInsertEventListener[] postInsertEventListener) {
			throw immutableError();
		}

		public PostCollectionRecreateEventListener[] getPostCollectionRecreateEventListeners() {
			return postCollectionRecreateEventListeners;
		}

		public void setPostCollectionRecreateEventListeners(PostCollectionRecreateEventListener[] postCollectionRecreateEventListener) {
			throw immutableError();
		}

		public PostCollectionRemoveEventListener[] getPostCollectionRemoveEventListeners() {
			return postCollectionRemoveEventListeners;
		}

		public void setPostCollectionRemoveEventListeners(PostCollectionRemoveEventListener[] postCollectionRemoveEventListener) {
			throw immutableError();
		}

		public PostCollectionUpdateEventListener[] getPostCollectionUpdateEventListeners() {
			return postCollectionUpdateEventListeners;
		}

		public void setPostCollectionUpdateEventListeners(PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners) {
			throw immutableError();
		}

		public PreDeleteEventListener[] getPreDeleteEventListeners() {
			return preDeleteEventListeners;
		}

		public void setPreDeleteEventListeners(PreDeleteEventListener[] preDeleteEventListener) {
			throw immutableError();
		}

		public PreInsertEventListener[] getPreInsertEventListeners() {
			return preInsertEventListeners;
		}

		public void setPreInsertEventListeners(PreInsertEventListener[] preInsertEventListener) {
			throw immutableError();
		}

		public PreUpdateEventListener[] getPreUpdateEventListeners() {
			return preUpdateEventListeners;
		}

		public void setPreUpdateEventListeners(PreUpdateEventListener[] preUpdateEventListener) {
			throw immutableError();
		}

		public PostDeleteEventListener[] getPostCommitDeleteEventListeners() {
			return postCommitDeleteEventListeners;
		}

		public void setPostCommitDeleteEventListeners(
				PostDeleteEventListener[] postCommitDeleteEventListeners) {
			throw immutableError();
		}

		public PostInsertEventListener[] getPostCommitInsertEventListeners() {
			return postCommitInsertEventListeners;
		}

		public void setPostCommitInsertEventListeners(PostInsertEventListener[] postCommitInsertEventListeners) {
			throw immutableError();
		}

		public PostUpdateEventListener[] getPostCommitUpdateEventListeners() {
			return postCommitUpdateEventListeners;
		}

		public void setPostCommitUpdateEventListeners(PostUpdateEventListener[] postCommitUpdateEventListeners) {
			throw immutableError();
		}
	}
}
