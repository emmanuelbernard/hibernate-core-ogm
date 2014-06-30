/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;

/**
 * Identifies a named association belonging to a particular
 * entity instance. Used to record the fact that an association
 * is null during loading.
 * 
 * @author Gavin King
 */
public final class AssociationKey implements Serializable {
	private EntityKey ownerKey;
	private String propertyName;

	/**
	 * Constructs an AssociationKey
	 *
	 * @param ownerKey The EntityKey of the association owner
	 * @param propertyName The name of the property on the owner which defines the association
	 */
	public AssociationKey(EntityKey ownerKey, String propertyName) {
		this.ownerKey = ownerKey;
		this.propertyName = propertyName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AssociationKey that = (AssociationKey) o;
		return ownerKey.equals( that.ownerKey )
				&& propertyName.equals( that.propertyName );
	}

	@Override
	public int hashCode() {
		int result = ownerKey.hashCode();
		result = 31 * result + propertyName.hashCode();
		return result;
	}
}
