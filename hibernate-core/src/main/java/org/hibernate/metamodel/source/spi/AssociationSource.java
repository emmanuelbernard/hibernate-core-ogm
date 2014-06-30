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
package org.hibernate.metamodel.source.spi;

import java.util.Set;

/**
 * @author Gail Badner
 */
public interface AssociationSource extends CascadeStyleSource {

	public AttributeSource getAttributeSource();

	/**
	 * Obtain the name of the referenced entity.
	 *
	 * @return The name of the referenced entity
	 */
	public String getReferencedEntityName();

	public boolean isIgnoreNotFound();

	/**
	 * Returns the attribute source that is owned by this {@link AssociationSource},
	 * if there is one.
	 * <p/>
	 * Specifically, this method returns the {@link AttributeSource} that is
	 * "mappedBy" this {@link AssociationSource}.
	 *
	 * @return
	 */
	public Set<MappedByAssociationSource> getOwnedAssociationSources();

	public void addMappedByAssociationSource(MappedByAssociationSource attributeSource);

	public boolean isMappedBy();
}
