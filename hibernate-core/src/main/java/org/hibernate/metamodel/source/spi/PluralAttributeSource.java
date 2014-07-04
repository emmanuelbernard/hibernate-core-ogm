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
package org.hibernate.metamodel.source.spi;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeSource
		extends AttributeSource, FetchableAttributeSource, PluralAttributeElementSourceResolver {
	public PluralAttributeNature getNature();

	public CollectionIdSource getCollectionIdSource();

	public PluralAttributeKeySource getKeySource();

	public PluralAttributeElementSource getElementSource();

	public FilterSource[] getFilterSources();

	public JavaTypeDescriptor getElementTypeDescriptor();

	public TableSpecificationSource getCollectionTableSpecificationSource();

	public String getCollectionTableComment();

	public String getCollectionTableCheck();

	public Caching getCaching();

	public String getCustomPersisterClassName();

	public String getWhere();

	public boolean isInverse();

	public boolean isMutable();

	public String getCustomLoaderName();

	public CustomSQL getCustomSqlInsert();

	public CustomSQL getCustomSqlUpdate();

	public CustomSQL getCustomSqlDelete();

	public CustomSQL getCustomSqlDeleteAll();

	public String getMappedBy();

	public int getBatchSize();

	public boolean usesJoinTable();
	
	public PluralAttribute getPluralAttribute();

}
