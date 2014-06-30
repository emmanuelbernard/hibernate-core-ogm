/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.entity.PropertyMapping;

/**
 * Defines a persister reference (either entity or collection) or a composite reference. In JPA terms this is
 * an "abstract schema type" when discussing JPQL or JPA Criteria queries.  This models a single source of attributes
 * (and fetches).
 *
 * @author Steve Ebersole
 */
public interface QuerySpace {
	/**
	 * The uid/alias which uniquely identifies this QuerySpace.  Can be used to uniquely reference this
	 * QuerySpace elsewhere.
	 *
	 * @return The uid
	 *
	 * @see QuerySpaces#findQuerySpaceByUid(java.lang.String)
	 */
	public String getUid();

	/**
	 * Get the {@link QuerySpaces} object that is our owner.
	 *
	 * @return The QuerySpaces containing this QuerySpace
	 */
	public QuerySpaces getQuerySpaces();

	/**
	 * Get the {@link PropertyMapping} for this QuerySpace.
	 *
	 * @return The PropertyMapping
	 */
	public PropertyMapping getPropertyMapping();

	/**
	 * Get the aliased column names for the specified property in the query space..
	 *
	 * @return the aliased column names for the specified property
	 * @param alias - the table alias
	 * @param propertyName - the property name
	 */
	public String[] toAliasedColumns(String alias, String propertyName);

	/**
	 * Enumeration of the different types of QuerySpaces we can have.
	 */
	public static enum Disposition {
		/**
		 * We have an entity-based QuerySpace.  It is castable to {@link EntityQuerySpace} for more details.
		 */
		ENTITY,
		/**
		 * We have a collection-based QuerySpace.  It is castable to {@link CollectionQuerySpace} for more details.
		 */
		COLLECTION,
		/**
		 * We have a composition-based QuerySpace.  It is castable to {@link CompositeQuerySpace} for more details.
		 */
		COMPOSITE
	}

	/**
	 * What type of QuerySpace (more-specific) is this?
	 *
	 * @return The enum value representing the more-specific type of QuerySpace
	 */
	public Disposition getDisposition();

	/**
	 * Obtain all joins which originate from this QuerySpace, in other words, all the joins which this QuerySpace is
	 * the left-hand-side of.
	 * <p/>
	 * For all the joins returned here, {@link Join#getLeftHandSide()} should point back to this QuerySpace such that
	 * <code>
	 *     space.getJoins().forEach{ join -> join.getLeftHandSide() == space }
	 * </code>
	 * is true for all.
	 *
	 * @return The joins which originate from this query space.
	 */
	public Iterable<Join> getJoins();
}
