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
package org.hibernate.cache.spi.entry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Structured CacheEntry format for persistent collections (other than Maps, see {@link StructuredMapCacheEntry}).
 *
 * @author Gavin King
 */
public class StructuredCollectionCacheEntry implements CacheEntryStructure {
	/**
	 * Access to the singleton reference.
	 */
	public static final StructuredCollectionCacheEntry INSTANCE = new StructuredCollectionCacheEntry();

	@Override
	public Object structure(Object item) {
		final CollectionCacheEntry entry = (CollectionCacheEntry) item;
		return Arrays.asList( entry.getState() );
	}

	@Override
	public Object destructure(Object structured, SessionFactoryImplementor factory) {
		final List list = (List) structured;
		return new CollectionCacheEntry( list.toArray( new Serializable[list.size()] ) );
	}

	private StructuredCollectionCacheEntry() {
	}
}
