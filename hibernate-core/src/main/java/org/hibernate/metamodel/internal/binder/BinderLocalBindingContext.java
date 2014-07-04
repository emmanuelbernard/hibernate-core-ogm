/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.binder;


import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;

/**
 * The processes performed by Binder are done as a series of steps, where each
 * step performs a iteration on an entity hierarchy and visits the hierarchy and
 * its entities.
 * <p/>
 * This contract representations access to "context information" relating to the
 * visitation and is presented to the visitation strategies.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface BinderLocalBindingContext extends BinderRootContext, LocalBindingContext {
	/**
	 * Locate the binding representation of the hierarchy for the given source
	 * representation.
	 *
	 * @param source The source representation
	 *
	 * @return The binding representation
	 */
	public HierarchyDetails locateBinding(EntityHierarchySource source);

	/**
	 * Locate the binding representation of the entity for the given source
	 * representation.
	 *
	 * @param source The source representation
	 *
	 * @return The binding representation
	 */
	public EntityBinding locateBinding(EntitySource source);

	HibernateTypeHelper typeHelper();
	RelationalIdentifierHelper relationalIdentifierHelper();
	TableHelper tableHelper();
	ForeignKeyHelper foreignKeyHelper();
	RelationalValueBindingHelper relationalValueBindingHelper();
	NaturalIdUniqueKeyHelper naturalIdUniqueKeyHelper();
}
