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
package org.hibernate.tuple.component;

import java.util.Iterator;

import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.tuple.AbstractNonIdentifierAttribute;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.JoinHelper.getLHSColumnNames;
import static org.hibernate.engine.internal.JoinHelper.getLHSTableName;
import static org.hibernate.engine.internal.JoinHelper.getRHSColumnNames;

/**
 * A base class for a composite, non-identifier attribute.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCompositionAttribute
		extends AbstractNonIdentifierAttribute
		implements CompositionDefinition {

	private final int columnStartPosition;

	protected AbstractCompositionAttribute(
			AttributeSource source,
			SessionFactoryImplementor sessionFactory,
			int entityBasedAttributeNumber,
			String attributeName,
			CompositeType attributeType,
			int columnStartPosition,
			BaselineAttributeInformation baselineInfo) {
		super( source, sessionFactory, entityBasedAttributeNumber, attributeName, attributeType, baselineInfo );
		this.columnStartPosition = columnStartPosition;
	}

	@Override
	public CompositeType getType() {
		return (CompositeType) super.getType();
	}

	@Override
	public Iterable<AttributeDefinition> getAttributes() {
		return new Iterable<AttributeDefinition>() {
			@Override
			public Iterator<AttributeDefinition> iterator() {
				return new Iterator<AttributeDefinition>() {
					private final int numberOfAttributes = getType().getSubtypes().length;
					private int currentSubAttributeNumber;
					private int currentColumnPosition = columnStartPosition;

					@Override
					public boolean hasNext() {
						return currentSubAttributeNumber < numberOfAttributes;
					}

					@Override
					public AttributeDefinition next() {
						final int subAttributeNumber = currentSubAttributeNumber;
						currentSubAttributeNumber++;

						final String name = getType().getPropertyNames()[subAttributeNumber];
						final Type type = getType().getSubtypes()[subAttributeNumber];

						int columnPosition = currentColumnPosition;
						currentColumnPosition += type.getColumnSpan( sessionFactory() );

						if ( type.isAssociationType() ) {
							// we build the association-key here because of the "goofiness" with 'currentColumnPosition'
							final AssociationKey associationKey;
							final AssociationType aType = (AssociationType) type;
							final Joinable joinable = aType.getAssociatedJoinable( sessionFactory() );

							if ( aType.isAnyType() ) {
								associationKey = new AssociationKey(
										JoinHelper.getLHSTableName(
												aType,
												attributeNumber(),
												(OuterJoinLoadable) locateOwningPersister()
										),
										JoinHelper.getLHSColumnNames(
												aType,
												attributeNumber(),
												columnPosition,
												(OuterJoinLoadable) locateOwningPersister(),
												sessionFactory()
										)
								);
							}
							else if ( aType.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT ) {
								final String lhsTableName;
								final String[] lhsColumnNames;

								if ( joinable.isCollection() ) {
									final QueryableCollection collectionPersister = (QueryableCollection) joinable;
									lhsTableName = collectionPersister.getTableName();
									lhsColumnNames = collectionPersister.getElementColumnNames();
								}
								else {
									final OuterJoinLoadable entityPersister = (OuterJoinLoadable) locateOwningPersister();
									lhsTableName = getLHSTableName( aType, attributeNumber(), entityPersister );
									lhsColumnNames = getLHSColumnNames(
											aType,
											attributeNumber(),
											columnPosition,
											entityPersister,
											sessionFactory()
									);
								}
								associationKey = new AssociationKey( lhsTableName, lhsColumnNames );
							}
							else {
								associationKey = new AssociationKey(
										joinable.getTableName(),
										getRHSColumnNames( aType, sessionFactory() )
								);
							}

							final CompositeType cType = getType();
							final boolean nullable =
									cType.getPropertyNullability() == null ||
											cType.getPropertyNullability()[subAttributeNumber];

							return new CompositeBasedAssociationAttribute(
									AbstractCompositionAttribute.this,
									sessionFactory(),
									attributeNumber(),
									name,
									(AssociationType) type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositionAttribute.this.isInsertable() )
											.setUpdateable( AbstractCompositionAttribute.this.isUpdateable() )
											// todo : handle nested ValueGeneration strategies...
											//		disallow if our strategy != NEVER
											.setNullable( nullable )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositionAttribute.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( subAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( subAttributeNumber ) )
											.createInformation(),
									subAttributeNumber,
									associationKey
							);
						}
						else if ( type.isComponentType() ) {
							return new CompositionBasedCompositionAttribute(
									AbstractCompositionAttribute.this,
									sessionFactory(),
									attributeNumber(),
									name,
									(CompositeType) type,
									columnPosition,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositionAttribute.this.isInsertable() )
											.setUpdateable( AbstractCompositionAttribute.this.isUpdateable() )
											// todo : handle nested ValueGeneration strategies...
											//		disallow if our strategy != NEVER
											.setNullable( getType().getPropertyNullability()[subAttributeNumber] )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositionAttribute.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( subAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( subAttributeNumber ) )
											.createInformation()
							);
						}
						else {
							final CompositeType cType = getType();
							final boolean nullable = cType.getPropertyNullability() == null || cType.getPropertyNullability()[subAttributeNumber];

							return new CompositeBasedBasicAttribute(
									AbstractCompositionAttribute.this,
									sessionFactory(),
									subAttributeNumber,
									name,
									type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositionAttribute.this.isInsertable() )
											.setUpdateable( AbstractCompositionAttribute.this.isUpdateable() )
											// todo : handle nested ValueGeneration strategies...
											//		disallow if our strategy != NEVER
											.setNullable( nullable )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositionAttribute.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( subAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( subAttributeNumber ) )
											.createInformation()
							);
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException( "Remove operation not supported here" );
					}
				};
			}
		};
	}

	protected abstract EntityPersister locateOwningPersister();

	@Override
	protected String loggableMetadata() {
		return super.loggableMetadata() + ",composition";
	}
}

