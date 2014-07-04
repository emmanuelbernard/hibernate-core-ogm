/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.jpamodelgen.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.ImportContextImpl;
import org.hibernate.jpamodelgen.model.ImportContext;
import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * Class used to collect meta information about an annotated type (entity, embeddable or mapped superclass).
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaEntity implements MetaEntity {

	private final ImportContext importContext;
	private final TypeElement element;
	private final Map<String, MetaAttribute> members;
	private final Context context;

	private AccessTypeInformation entityAccessTypeInfo;

	/**
	 * Whether the members of this type have already been initialized or not.
	 * <p>
	 * Embeddables and mapped super-classes need to be lazily initialized since the access type may be determined by
	 * the class which is embedding or sub-classing the entity or super-class. This might not be known until
	 * annotations are processed.
	 * <p>
	 * Also note, that if two different classes with different access types embed this entity or extend this mapped
	 * super-class, the access type of the embeddable/super-class will be the one of the last embedding/sub-classing
	 * entity processed. The result is not determined (that's ok according to the spec).
	 */
	private boolean initialized;

	/**
	 * Another meta entity for the same type which should be merged lazily with this meta entity. Doing the merge
	 * lazily is required for embeddedables and mapped supertypes to only pull in those members matching the access
	 * type as configured via the embedding entity or subclass (also see METAGEN-85).
	 */
	private MetaEntity entityToMerge;

	public AnnotationMetaEntity(TypeElement element, Context context, boolean lazilyInitialised) {
		this.element = element;
		this.context = context;
		this.members = new HashMap<String, MetaAttribute>();
		this.importContext = new ImportContextImpl( getPackageName() );
		if ( !lazilyInitialised ) {
			init();
		}
	}

	public AccessTypeInformation getEntityAccessTypeInfo() {
		return entityAccessTypeInfo;
	}

	public final Context getContext() {
		return context;
	}

	public final String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public final String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public final String getPackageName() {
		PackageElement packageOf = context.getElementUtils().getPackageOf( element );
		return context.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<MetaAttribute> getMembers() {
		if ( !initialized ) {
			init();
			if ( entityToMerge != null ) {
				mergeInMembers( entityToMerge.getMembers() );
			}
		}

		return new ArrayList<MetaAttribute>( members.values() );
	}

	@Override
	public boolean isMetaComplete() {
		return false;
	}

	private void mergeInMembers(Collection<MetaAttribute> attributes) {
		for ( MetaAttribute attribute : attributes ) {
			// propagate types to be imported
			importType( attribute.getMetaType() );
			importType( attribute.getTypeDeclaration() );

			members.put( attribute.getPropertyName(), attribute );
		}
	}

	public void mergeInMembers(MetaEntity other) {
		// store the entity in order do the merge lazily in case of a non-initialized embeddedable or mapped superclass
		if ( !initialized ) {
			this.entityToMerge = other;
		}
		else {
			mergeInMembers( other.getMembers() );
		}
	}

	public final String generateImports() {
		return importContext.generateImports();
	}

	public final String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public final String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public final TypeElement getTypeElement() {
		return element;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AnnotationMetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( ", members=" ).append( members );
		sb.append( '}' );
		return sb.toString();
	}

	protected TypeElement getElement() {
		return element;
	}

	protected final void init() {
		getContext().logMessage( Diagnostic.Kind.OTHER, "Initializing type " + getQualifiedName() + "." );

		TypeUtils.determineAccessTypeForHierarchy( element, context );
		entityAccessTypeInfo = context.getAccessTypeInfo( getQualifiedName() );

		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		addPersistentMembers( fieldsOfClass, AccessType.FIELD );

		List<? extends Element> methodsOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		addPersistentMembers( methodsOfClass, AccessType.PROPERTY );

		initialized = true;
	}

	private void addPersistentMembers(List<? extends Element> membersOfClass, AccessType membersKind) {
		for ( Element memberOfClass : membersOfClass ) {
			AccessType forcedAccessType = TypeUtils.determineAnnotationSpecifiedAccessType( memberOfClass );
			if ( entityAccessTypeInfo.getAccessType() != membersKind && forcedAccessType == null ) {
				continue;
			}

			if ( TypeUtils.containsAnnotation( memberOfClass, Constants.TRANSIENT )
					|| memberOfClass.getModifiers().contains( Modifier.TRANSIENT )
					|| memberOfClass.getModifiers().contains( Modifier.STATIC ) ) {
				continue;
			}

			MetaAttributeGenerationVisitor visitor = new MetaAttributeGenerationVisitor( this, context );
			AnnotationMetaAttribute result = memberOfClass.asType().accept( visitor, memberOfClass );
			if ( result != null ) {
				members.put( result.getPropertyName(), result );
			}
		}
	}
}
