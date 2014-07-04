/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains locking details (LockMode, Timeout and Scope).
 * 
 * @author Scott Marlow
 */
public class LockOptions implements Serializable {
	/**
	 * Represents LockMode.NONE (timeout + scope do not apply).
	 */
	public static final LockOptions NONE = new LockOptions(LockMode.NONE);

	/**
	 * Represents LockMode.READ (timeout + scope do not apply).
	 */
	public static final LockOptions READ = new LockOptions(LockMode.READ);

	/**
	 * Represents LockMode.UPGRADE (will wait forever for lock and scope of false meaning only entity is locked).
	 */
	@SuppressWarnings("deprecation")
	public static final LockOptions UPGRADE = new LockOptions(LockMode.UPGRADE);

	/**
	 * Indicates that the database should not wait at all to acquire the pessimistic lock.
	 * @see #getTimeOut
	 */
	public static final int NO_WAIT = 0;

	/**
	 * Indicates that there is no timeout for the acquisition.
	 * @see #getTimeOut
	 */
	public static final int WAIT_FOREVER = -1;

	/**
	 * Indicates that rows that are already locked should be skipped.
	 * @see #getTimeOut()
	 */
	public static final int SKIP_LOCKED = -2;

	private LockMode lockMode = LockMode.NONE;
	private int timeout = WAIT_FOREVER;

	private Map<String,LockMode> aliasSpecificLockModes;

	/**
	 * Constructs a LockOptions with all default options.
	 */
	public LockOptions() {
	}

	/**
	 * Constructs a LockOptions with the given lock mode.
	 *
	 * @param lockMode The lock mode to use
	 */
	public LockOptions( LockMode lockMode) {
		this.lockMode = lockMode;
	}


	/**
	 * Retrieve the overall lock mode in effect for this set of options.
	 * <p/>
	 * In certain contexts (hql and criteria), lock-modes can be defined in an
	 * even more granular {@link #setAliasSpecificLockMode(String, LockMode) per-alias} fashion
	 *
	 * @return The overall lock mode.
	 */
	public LockMode getLockMode() {
		return lockMode;
	}

	/**
	 * Set the overall {@link LockMode} to be used.  The default is
	 * {@link LockMode#NONE}
	 *
	 * @param lockMode The new overall lock mode to use.
	 *
	 * @return this (for method chaining).
	 */
	public LockOptions setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	/**
	 * Specify the {@link LockMode} to be used for a specific query alias.
	 *
	 * @param alias used to reference the LockMode.
	 * @param lockMode The lock mode to apply to the given alias
	 * @return this LockRequest instance for operation chaining.
	 *
	 * @see Query#setLockMode(String, LockMode)
	 * @see Criteria#setLockMode(LockMode)
	 * @see Criteria#setLockMode(String, LockMode)
	 */
	public LockOptions setAliasSpecificLockMode(String alias, LockMode lockMode) {
		if ( aliasSpecificLockModes == null ) {
			aliasSpecificLockModes = new HashMap<String,LockMode>();
		}
		aliasSpecificLockModes.put( alias, lockMode );
		return this;
	}

	/**
	 * Get the {@link LockMode} explicitly specified for the given alias via
	 * {@link #setAliasSpecificLockMode}
	 * <p/>
	 * Differs from {@link #getEffectiveLockMode} in that here we only return
	 * explicitly specified alias-specific lock modes.
	 *
	 * @param alias The alias for which to locate the explicit lock mode.
	 *
	 * @return The explicit lock mode for that alias.
	 */
	public LockMode getAliasSpecificLockMode(String alias) {
		if ( aliasSpecificLockModes == null ) {
			return null;
		}
		return aliasSpecificLockModes.get( alias );
	}

	/**
	 * Determine the {@link LockMode} to apply to the given alias.  If no
	 * mode was explicitly {@link #setAliasSpecificLockMode set}, the
	 * {@link #getLockMode overall mode} is returned.  If the overall lock mode is
	 * <tt>null</tt> as well, {@link LockMode#NONE} is returned.
	 * <p/>
	 * Differs from {@link #getAliasSpecificLockMode} in that here we fallback to we only return
	 * the overall lock mode.
	 *
	 * @param alias The alias for which to locate the effective lock mode.
	 *
	 * @return The effective lock mode.
	 */
	public LockMode getEffectiveLockMode(String alias) {
		LockMode lockMode = getAliasSpecificLockMode( alias );
		if ( lockMode == null ) {
			lockMode = this.lockMode;
		}
		return lockMode == null ? LockMode.NONE : lockMode;
	}

	/**
	 * Does this LockOptions object define alias-specific lock modes?
	 *
	 * @return {@code true} if this LockOptions object define alias-specific lock modes; {@code false} otherwise.
	 */
	public boolean hasAliasSpecificLockModes() {
		return aliasSpecificLockModes != null
				&& ! aliasSpecificLockModes.isEmpty();
	}

	/**
	 * Get the number of aliases that have specific lock modes defined.
	 *
	 * @return the number of explicitly defined alias lock modes.
	 */
	public int getAliasLockCount() {
		if ( aliasSpecificLockModes == null ) {
			return 0;
		}
		return aliasSpecificLockModes.size();
	}

	/**
	 * Iterator for accessing Alias (key) and LockMode (value) as Map.Entry.
	 *
	 * @return Iterator for accessing the Map.Entry's
	 */
	public Iterator getAliasLockIterator() {
		if ( aliasSpecificLockModes == null ) {
			return Collections.emptyList().iterator();
		}
		return aliasSpecificLockModes.entrySet().iterator();
	}

	/**
	 * Currently needed for follow-on locking.
	 *
	 * @return The greatest of all requested lock modes.
	 */
	public LockMode findGreatestLockMode() {
		LockMode lockModeToUse = getLockMode();
		if ( lockModeToUse == null ) {
			lockModeToUse = LockMode.NONE;
		}

		if ( aliasSpecificLockModes == null ) {
			return lockModeToUse;
		}

		for ( LockMode lockMode : aliasSpecificLockModes.values() ) {
			if ( lockMode.greaterThan( lockModeToUse ) ) {
				lockModeToUse = lockMode;
			}
		}

		return lockModeToUse;
	}

	/**
	 * Retrieve the current timeout setting.
	 * <p/>
	 * The timeout is the amount of time, in milliseconds, we should instruct the database
	 * to wait for any requested pessimistic lock acquisition.
	 * <p/>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER} or {@link #SKIP_LOCKED} represent 3 "magic" values.
	 *
	 * @return timeout in milliseconds, {@link #NO_WAIT}, {@link #WAIT_FOREVER} or {@link #SKIP_LOCKED}
	 */
	public int getTimeOut() {
		return timeout;
	}

	/**
	 * Set the timeout setting.
	 * <p/>
	 * See {@link #getTimeOut} for a discussion of meaning.
	 *
	 * @param timeout The new timeout setting.
	 *
	 * @return this (for method chaining).
	 *
	 * @see #getTimeOut
	 */
	public LockOptions setTimeOut(int timeout) {
		this.timeout = timeout;
		return this;
	}

	private boolean scope;

	/**
	 * Retrieve the current lock scope setting.
	 * <p/>
	 * "scope" is a JPA defined term.  It is basically a cascading of the lock to associations.
	 *
	 * @return true if locking will be extended to owned associations
	 */
	public boolean getScope() {
		return scope;
	}

	/**
	 * Set the scope.
	 *
	 * @param scope The new scope setting
	 *
	 * @return this (for method chaining).
	 */
	public LockOptions setScope(boolean scope) {
		this.scope = scope;
		return this;
	}

	/**
	 * Make a copy.
	 *
	 * @return The copy
	 */
	public LockOptions makeCopy() {
		final LockOptions copy = new LockOptions();
		copy( this, copy );
		return copy;
	}

	/**
	 * Perform a shallow copy.
	 *
	 * @param source Source for the copy (copied from)
	 * @param destination Destination for the copy (copied to)
	 *
	 * @return destination
	 */
	public static LockOptions copy(LockOptions source, LockOptions destination) {
		destination.setLockMode( source.getLockMode() );
		destination.setScope( source.getScope() );
		destination.setTimeOut( source.getTimeOut() );
		if ( source.aliasSpecificLockModes != null ) {
			destination.aliasSpecificLockModes = new HashMap<String,LockMode>( source.aliasSpecificLockModes );
		}
		return destination;
	}
}
