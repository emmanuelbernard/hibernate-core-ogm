/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.internal.strategy;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.regions.EhcacheTransactionalDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

import org.jboss.logging.Logger;

/**
 * Superclass for all Ehcache specific read/write AccessStrategy implementations.
 *
 * @param <T> the type of the enclosed cache region
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
abstract class AbstractReadWriteEhcacheAccessStrategy<T
		extends EhcacheTransactionalDataRegion>
		extends AbstractEhcacheAccessStrategy<T> {

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			AbstractReadWriteEhcacheAccessStrategy.class.getName()
	);

	private final UUID uuid = UUID.randomUUID();
	private final AtomicLong nextLockId = new AtomicLong();

	private final Comparator versionComparator;

	/**
	 * Creates a read/write cache access strategy around the given cache region.
	 */
	public AbstractReadWriteEhcacheAccessStrategy(T region, Settings settings) {
		super( region, settings );
		this.versionComparator = region.getCacheDataDescription().getVersionComparator();
	}

	/**
	 * Returns <code>null</code> if the item is not readable.  Locked items are not readable, nor are items created
	 * after the start of this transaction.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#get(java.lang.Object, long)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#get(java.lang.Object, long)
	 */
	public final Object get(Object key, long txTimestamp) throws CacheException {
		readLockIfNeeded( key );
		try {
			final Lockable item = (Lockable) region().get( key );

			final boolean readable = item != null && item.isReadable( txTimestamp );
			if ( readable ) {
				return item.getValue();
			}
			else {
				return null;
			}
		}
		finally {
			readUnlockIfNeeded( key );
		}
	}

	/**
	 * Returns <code>false</code> and fails to put the value if there is an existing un-writeable item mapped to this
	 * key.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)
	 */
	@Override
	public final boolean putFromLoad(
			Object key,
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride)
			throws CacheException {
		region().writeLock( key );
		try {
			final Lockable item = (Lockable) region().get( key );
			final boolean writeable = item == null || item.isWriteable( txTimestamp, version, versionComparator );
			if ( writeable ) {
				region().put( key, new Item( value, version, region().nextTimestamp() ) );
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			region().writeUnlock( key );
		}
	}

	/**
	 * Soft-lock a cache item.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#lockItem(java.lang.Object, java.lang.Object)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#lockItem(java.lang.Object, java.lang.Object)
	 */
	public final SoftLock lockItem(Object key, Object version) throws CacheException {
		region().writeLock( key );
		try {
			final Lockable item = (Lockable) region().get( key );
			final long timeout = region().nextTimestamp() + region().getTimeout();
			final Lock lock = (item == null) ? new Lock( timeout, uuid, nextLockId(), version ) : item.lock(
					timeout,
					uuid,
					nextLockId()
			);
			region().put( key, lock );
			return lock;
		}
		finally {
			region().writeUnlock( key );
		}
	}

	/**
	 * Soft-unlock a cache item.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#unlockItem(java.lang.Object, org.hibernate.cache.spi.access.SoftLock)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#unlockItem(java.lang.Object, org.hibernate.cache.spi.access.SoftLock)
	 */
	public final void unlockItem(Object key, SoftLock lock) throws CacheException {
		region().writeLock( key );
		try {
			final Lockable item = (Lockable) region().get( key );

			if ( (item != null) && item.isUnlockable( lock ) ) {
				decrementLock( key, (Lock) item );
			}
			else {
				handleLockExpiry( key, item );
			}
		}
		finally {
			region().writeUnlock( key );
		}
	}

	private long nextLockId() {
		return nextLockId.getAndIncrement();
	}

	/**
	 * Unlock and re-put the given key, lock combination.
	 */
	protected void decrementLock(Object key, Lock lock) {
		lock.unlock( region().nextTimestamp() );
		region().put( key, lock );
	}

	/**
	 * Handle the timeout of a previous lock mapped to this key
	 */
	protected void handleLockExpiry(Object key, Lockable lock) {
		LOG.softLockedCacheExpired( region().getName(), key, lock == null ? "(null)" : lock.toString() );

		final long ts = region().nextTimestamp() + region().getTimeout();
		// create new lock that times out immediately
		final Lock newLock = new Lock( ts, uuid, nextLockId.getAndIncrement(), null );
		newLock.unlock( ts );
		region().put( key, newLock );
	}

	/**
	 * Read lock the entry for the given key if internal cache locks will not provide correct exclusion.
	 */
	private void readLockIfNeeded(Object key) {
		if ( region().locksAreIndependentOfCache() ) {
			region().readLock( key );
		}
	}

	/**
	 * Read unlock the entry for the given key if internal cache locks will not provide correct exclusion.
	 */
	private void readUnlockIfNeeded(Object key) {
		if ( region().locksAreIndependentOfCache() ) {
			region().readUnlock( key );
		}
	}

	/**
	 * Interface type implemented by all wrapper objects in the cache.
	 */
	protected static interface Lockable {

		/**
		 * Returns <code>true</code> if the enclosed value can be read by a transaction started at the given time.
		 */
		public boolean isReadable(long txTimestamp);

		/**
		 * Returns <code>true</code> if the enclosed value can be replaced with one of the given version by a
		 * transaction started at the given time.
		 */
		public boolean isWriteable(long txTimestamp, Object version, Comparator versionComparator);

		/**
		 * Returns the enclosed value.
		 */
		public Object getValue();

		/**
		 * Returns <code>true</code> if the given lock can be unlocked using the given SoftLock instance as a handle.
		 */
		public boolean isUnlockable(SoftLock lock);

		/**
		 * Locks this entry, stamping it with the UUID and lockId given, with the lock timeout occuring at the specified
		 * time.  The returned Lock object can be used to unlock the entry in the future.
		 */
		public Lock lock(long timeout, UUID uuid, long lockId);
	}

	/**
	 * Wrapper type representing unlocked items.
	 */
	protected static final class Item implements Serializable, Lockable {
		private static final long serialVersionUID = 1L;
		private final Object value;
		private final Object version;
		private final long timestamp;

		/**
		 * Creates an unlocked item wrapping the given value with a version and creation timestamp.
		 */
		Item(Object value, Object version, long timestamp) {
			this.value = value;
			this.version = version;
			this.timestamp = timestamp;
		}

		@Override
		public boolean isReadable(long txTimestamp) {
			return txTimestamp > timestamp;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			return version != null && versionComparator.compare( version, newVersion ) < 0;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public boolean isUnlockable(SoftLock lock) {
			return false;
		}

		@Override
		public Lock lock(long timeout, UUID uuid, long lockId) {
			return new Lock( timeout, uuid, lockId, version );
		}
	}

	/**
	 * Wrapper type representing locked items.
	 */
	protected static final class Lock implements Serializable, Lockable, SoftLock {
		private static final long serialVersionUID = 2L;

		private final UUID sourceUuid;
		private final long lockId;
		private final Object version;

		private long timeout;
		private boolean concurrent;
		private int multiplicity = 1;
		private long unlockTimestamp;

		/**
		 * Creates a locked item with the given identifiers and object version.
		 */
		Lock(long timeout, UUID sourceUuid, long lockId, Object version) {
			this.timeout = timeout;
			this.lockId = lockId;
			this.version = version;
			this.sourceUuid = sourceUuid;
		}

		@Override
		public boolean isReadable(long txTimestamp) {
			return false;
		}

		@Override
		@SuppressWarnings({"SimplifiableIfStatement", "unchecked"})
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			if ( txTimestamp > timeout ) {
				// if timedout then allow write
				return true;
			}
			if ( multiplicity > 0 ) {
				// if still locked then disallow write
				return false;
			}
			return version == null
					? txTimestamp > unlockTimestamp
					: versionComparator.compare( version, newVersion ) < 0;
		}

		@Override
		public Object getValue() {
			return null;
		}

		@Override
		public boolean isUnlockable(SoftLock lock) {
			return equals( lock );
		}

		@Override
		@SuppressWarnings("SimplifiableIfStatement")
		public boolean equals(Object o) {
			if ( o == this ) {
				return true;
			}
			else if ( o instanceof Lock ) {
				return (lockId == ((Lock) o).lockId) && sourceUuid.equals( ((Lock) o).sourceUuid );
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			final int hash = (sourceUuid != null ? sourceUuid.hashCode() : 0);
			int temp = (int) lockId;
			for ( int i = 1; i < Long.SIZE / Integer.SIZE; i++ ) {
				temp ^= (lockId >>> (i * Integer.SIZE));
			}
			return hash + temp;
		}

		/**
		 * Returns true if this Lock has been concurrently locked by more than one transaction.
		 */
		public boolean wasLockedConcurrently() {
			return concurrent;
		}

		@Override
		public Lock lock(long timeout, UUID uuid, long lockId) {
			concurrent = true;
			multiplicity++;
			this.timeout = timeout;
			return this;
		}

		/**
		 * Unlocks this Lock, and timestamps the unlock event.
		 */
		public void unlock(long timestamp) {
			if ( --multiplicity == 0 ) {
				unlockTimestamp = timestamp;
			}
		}

		@Override
		public String toString() {
			return "Lock Source-UUID:" + sourceUuid + " Lock-ID:" + lockId;
		}
	}
}

