/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.query.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.QueryParameterException;
import org.hibernate.type.Type;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadata implements Serializable {
	private static final OrdinalParameterDescriptor[] EMPTY_ORDINALS = new OrdinalParameterDescriptor[0];

	private final OrdinalParameterDescriptor[] ordinalDescriptors;
	private final Map<String,NamedParameterDescriptor> namedDescriptorMap;

	/**
	 * Instantiates a ParameterMetadata container.
	 *
	 * @param ordinalDescriptors Descriptors of the ordinal parameters
	 * @param namedDescriptorMap Descriptors of the named parameters
	 */
	public ParameterMetadata(
			OrdinalParameterDescriptor[] ordinalDescriptors,
			Map<String,NamedParameterDescriptor> namedDescriptorMap) {
		if ( ordinalDescriptors == null ) {
			this.ordinalDescriptors = EMPTY_ORDINALS;
		}
		else {
			final OrdinalParameterDescriptor[] copy = new OrdinalParameterDescriptor[ ordinalDescriptors.length ];
			System.arraycopy( ordinalDescriptors, 0, copy, 0, ordinalDescriptors.length );
			this.ordinalDescriptors = copy;
		}

		if ( namedDescriptorMap == null ) {
			this.namedDescriptorMap = java.util.Collections.emptyMap();
		}
		else {
			final int size = (int) ( ( namedDescriptorMap.size() / .75 ) + 1 );
			final Map<String,NamedParameterDescriptor> copy = new HashMap<String,NamedParameterDescriptor>( size );
			copy.putAll( namedDescriptorMap );
			this.namedDescriptorMap = java.util.Collections.unmodifiableMap( copy );
		}
	}

	public int getOrdinalParameterCount() {
		return ordinalDescriptors.length;
	}

	/**
	 * Get the descriptor for an ordinal parameter given its position
	 *
	 * @param position The position (1 based)
	 *
	 * @return The ordinal parameter descriptor
	 *
	 * @throws QueryParameterException If the position is out of range
	 */
	public OrdinalParameterDescriptor getOrdinalParameterDescriptor(int position) {
		if ( position < 1 || position > ordinalDescriptors.length ) {
			throw new QueryParameterException(
					"Position beyond number of declared ordinal parameters. " +
							"Remember that ordinal parameters are 1-based! Position: " + position
			);
		}
		return ordinalDescriptors[position - 1];
	}

	/**
	 * Deprecated.
	 *
	 * @param position The position
	 *
	 * @return The type
	 *
	 * @deprecated Use {@link OrdinalParameterDescriptor#getExpectedType()} from the
	 * {@link #getOrdinalParameterDescriptor} return instead
	 */
	@Deprecated
	public Type getOrdinalParameterExpectedType(int position) {
		return getOrdinalParameterDescriptor( position ).getExpectedType();
	}

	/**
	 * Deprecated.
	 *
	 * @param position The position
	 *
	 * @return The source location
	 *
	 * @deprecated Use {@link OrdinalParameterDescriptor#getSourceLocation()} from the
	 * {@link #getOrdinalParameterDescriptor} return instead
	 */
	@Deprecated
	public int getOrdinalParameterSourceLocation(int position) {
		return getOrdinalParameterDescriptor( position ).getSourceLocation();
	}

	/**
	 * Access to the names of all named parameters
	 *
	 * @return The named parameter names
	 */
	public Set<String> getNamedParameterNames() {
		return namedDescriptorMap.keySet();
	}

	/**
	 * Get the descriptor for a named parameter given the name
	 *
	 * @param name The name of the parameter to locate
	 *
	 * @return The named parameter descriptor
	 *
	 * @throws QueryParameterException If the name could not be resolved to a named parameter
	 */
	public NamedParameterDescriptor getNamedParameterDescriptor(String name) {
		final NamedParameterDescriptor meta = namedDescriptorMap.get( name );
		if ( meta == null ) {
			throw new QueryParameterException( "could not locate named parameter [" + name + "]" );
		}
		return meta;
	}

	/**
	 * Deprecated.
	 *
	 * @param name The name of the parameter
	 *
	 * @return The type
	 *
	 * @deprecated Use {@link NamedParameterDescriptor#getExpectedType()} from the
	 * {@link #getNamedParameterDescriptor} return instead
	 */
	@Deprecated
	public Type getNamedParameterExpectedType(String name) {
		return getNamedParameterDescriptor( name ).getExpectedType();
	}

	/**
	 * Deprecated.
	 *
	 * @param name The name of the parameter
	 *
	 * @return The type
	 *
	 * @deprecated Use {@link NamedParameterDescriptor#getSourceLocations()} from the
	 * {@link #getNamedParameterDescriptor} return instead
	 */
	@Deprecated
	public int[] getNamedParameterSourceLocations(String name) {
		return getNamedParameterDescriptor( name ).getSourceLocations();
	}

}
