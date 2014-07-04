/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.annotations.reflection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AccessType;
import javax.persistence.AttributeConverter;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * A helper for consuming orm.xml mappings.
 *
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class XMLContext implements Serializable {
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger( XMLContext.class );

	private Default globalDefaults;
	private Map<String, Element> classOverriding = new HashMap<String, Element>();
	private Map<String, Default> defaultsOverriding = new HashMap<String, Default>();
	private List<Element> defaultElements = new ArrayList<Element>();
	private List<String> defaultEntityListeners = new ArrayList<String>();
	private boolean hasContext = false;

	/**
	 * @param doc The xml document to add
	 * @return Add a xml document to this context and return the list of added class names.
	 */
	@SuppressWarnings( "unchecked" )
	public List<String> addDocument(Document doc) {
		hasContext = true;
		List<String> addedClasses = new ArrayList<String>();
		Element root = doc.getRootElement();
		//global defaults
		Element metadata = root.element( "persistence-unit-metadata" );
		if ( metadata != null ) {
			if ( globalDefaults == null ) {
				globalDefaults = new Default();
				globalDefaults.setMetadataComplete(
						metadata.element( "xml-mapping-metadata-complete" ) != null ?
								Boolean.TRUE :
								null
				);
				Element defaultElement = metadata.element( "persistence-unit-defaults" );
				if ( defaultElement != null ) {
					Element unitElement = defaultElement.element( "schema" );
					globalDefaults.setSchema( unitElement != null ? unitElement.getTextTrim() : null );
					unitElement = defaultElement.element( "catalog" );
					globalDefaults.setCatalog( unitElement != null ? unitElement.getTextTrim() : null );
					unitElement = defaultElement.element( "access" );
					setAccess( unitElement, globalDefaults );
					unitElement = defaultElement.element( "cascade-persist" );
					globalDefaults.setCascadePersist( unitElement != null ? Boolean.TRUE : null );
					unitElement = defaultElement.element( "delimited-identifiers" );
					globalDefaults.setDelimitedIdentifiers( unitElement != null ? Boolean.TRUE : null );
					defaultEntityListeners.addAll( addEntityListenerClasses( defaultElement, null, addedClasses ) );
				}
			}
			else {
				LOG.duplicateMetadata();
			}
		}

		//entity mapping default
		Default entityMappingDefault = new Default();
		Element unitElement = root.element( "package" );
		String packageName = unitElement != null ? unitElement.getTextTrim() : null;
		entityMappingDefault.setPackageName( packageName );
		unitElement = root.element( "schema" );
		entityMappingDefault.setSchema( unitElement != null ? unitElement.getTextTrim() : null );
		unitElement = root.element( "catalog" );
		entityMappingDefault.setCatalog( unitElement != null ? unitElement.getTextTrim() : null );
		unitElement = root.element( "access" );
		setAccess( unitElement, entityMappingDefault );
		defaultElements.add( root );
		
		setLocalAttributeConverterDefinitions( root.elements( "converter" ) );

		List<Element> entities = root.elements( "entity" );
		addClass( entities, packageName, entityMappingDefault, addedClasses );

		entities = root.elements( "mapped-superclass" );
		addClass( entities, packageName, entityMappingDefault, addedClasses );

		entities = root.elements( "embeddable" );
		addClass( entities, packageName, entityMappingDefault, addedClasses );
		return addedClasses;
	}

	private void setAccess(Element unitElement, Default defaultType) {
		if ( unitElement != null ) {
			String access = unitElement.getTextTrim();
			setAccess( access, defaultType );
		}
	}

	private void setAccess( String access, Default defaultType) {
		AccessType type;
		if ( access != null ) {
			try {
				type = AccessType.valueOf( access );
			}
			catch ( IllegalArgumentException e ) {
				throw new AnnotationException( "Invalid access type " + access + " (check your xml configuration)" );
			}
			defaultType.setAccess( type );
		}
	}

	private void addClass(List<Element> entities, String packageName, Default defaults, List<String> addedClasses) {
		for (Element element : entities) {
			String className = buildSafeClassName( element.attributeValue( "class" ), packageName );
			if ( classOverriding.containsKey( className ) ) {
				//maybe switch it to warn?
				throw new IllegalStateException( "Duplicate XML entry for " + className );
			}
			addedClasses.add( className );
			classOverriding.put( className, element );
			Default localDefault = new Default();
			localDefault.override( defaults );
			String metadataCompleteString = element.attributeValue( "metadata-complete" );
			if ( metadataCompleteString != null ) {
				localDefault.setMetadataComplete( Boolean.parseBoolean( metadataCompleteString ) );
			}
			String access = element.attributeValue( "access" );
			setAccess( access, localDefault );
			defaultsOverriding.put( className, localDefault );

			LOG.debugf( "Adding XML overriding information for %s", className );
			addEntityListenerClasses( element, packageName, addedClasses );
		}
	}

	private List<String> addEntityListenerClasses(Element element, String packageName, List<String> addedClasses) {
		List<String> localAddedClasses = new ArrayList<String>();
		Element listeners = element.element( "entity-listeners" );
		if ( listeners != null ) {
			@SuppressWarnings( "unchecked" )
			List<Element> elements = listeners.elements( "entity-listener" );
			for (Element listener : elements) {
				String listenerClassName = buildSafeClassName( listener.attributeValue( "class" ), packageName );
				if ( classOverriding.containsKey( listenerClassName ) ) {
					//maybe switch it to warn?
					if ( "entity-listener".equals( classOverriding.get( listenerClassName ).getName() ) ) {
						LOG.duplicateListener( listenerClassName );
						continue;
					}
					throw new IllegalStateException("Duplicate XML entry for " + listenerClassName);
				}
				localAddedClasses.add( listenerClassName );
				classOverriding.put( listenerClassName, listener );
			}
		}
		LOG.debugf( "Adding XML overriding information for listeners: %s", localAddedClasses );
		addedClasses.addAll( localAddedClasses );
		return localAddedClasses;
	}
	
	@SuppressWarnings("unchecked")
	private void setLocalAttributeConverterDefinitions(List<Element> converterElements) {
		for ( Element converterElement : converterElements ) {
			final String className = converterElement.attributeValue( "class" );
			final String autoApplyAttribute = converterElement.attributeValue( "auto-apply" );
			final boolean autoApply = autoApplyAttribute != null && Boolean.parseBoolean( autoApplyAttribute );

			try {
				final Class<? extends AttributeConverter> attributeConverterClass = ReflectHelper.classForName(
						className
				);
				attributeConverterDefinitions.add(
						new AttributeConverterDefinition( attributeConverterClass.newInstance(), autoApply )
				);
			}
			catch (ClassNotFoundException e) {
				throw new AnnotationException( "Unable to locate specified AttributeConverter implementation class : " + className, e );
			}
			catch (Exception e) {
				throw new AnnotationException( "Unable to instantiate specified AttributeConverter implementation class : " + className, e );
			}
		}
	}

	public static String buildSafeClassName(String className, String defaultPackageName) {
		if ( className.indexOf( '.' ) < 0 && StringHelper.isNotEmpty( defaultPackageName ) ) {
			className = StringHelper.qualify( defaultPackageName, className );
		}
		return className;
	}

	public static String buildSafeClassName(String className, XMLContext.Default defaults) {
		return buildSafeClassName( className, defaults.getPackageName() );
	}

	public Default getDefault(String className) {
		Default xmlDefault = new Default();
		xmlDefault.override( globalDefaults );
		if ( className != null ) {
			Default entityMappingOverriding = defaultsOverriding.get( className );
			xmlDefault.override( entityMappingOverriding );
		}
		return xmlDefault;
	}

	public Element getXMLTree(String className ) {
		return classOverriding.get( className );
	}

	public List<Element> getAllDocuments() {
		return defaultElements;
	}

	public boolean hasContext() {
		return hasContext;
	}

	private List<AttributeConverterDefinition> attributeConverterDefinitions = new ArrayList<AttributeConverterDefinition>();

	public void applyDiscoveredAttributeConverters(Configuration configuration) {
		for ( AttributeConverterDefinition definition : attributeConverterDefinitions ) {
			configuration.addAttributeConverter( definition );
		}
		attributeConverterDefinitions.clear();
	}

	public static class Default implements Serializable {
		private AccessType access;
		private String packageName;
		private String schema;
		private String catalog;
		private Boolean metadataComplete;
		private Boolean cascadePersist;
		private Boolean delimitedIdentifier;

		public AccessType getAccess() {
			return access;
		}

		protected void setAccess(AccessType access) {
			this.access = access;
		}

		public String getCatalog() {
			return catalog;
		}

		protected void setCatalog(String catalog) {
			this.catalog = catalog;
		}

		public String getPackageName() {
			return packageName;
		}

		protected void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public String getSchema() {
			return schema;
		}

		protected void setSchema(String schema) {
			this.schema = schema;
		}

		public Boolean getMetadataComplete() {
			return metadataComplete;
		}

		public boolean canUseJavaAnnotations() {
			return metadataComplete == null || !metadataComplete;
		}

		protected void setMetadataComplete(Boolean metadataComplete) {
			this.metadataComplete = metadataComplete;
		}

		public Boolean getCascadePersist() {
			return cascadePersist;
		}

		void setCascadePersist(Boolean cascadePersist) {
			this.cascadePersist = cascadePersist;
		}

		public void override(Default globalDefault) {
			if ( globalDefault != null ) {
				if ( globalDefault.getAccess() != null ) access = globalDefault.getAccess();
				if ( globalDefault.getPackageName() != null ) packageName = globalDefault.getPackageName();
				if ( globalDefault.getSchema() != null ) schema = globalDefault.getSchema();
				if ( globalDefault.getCatalog() != null ) catalog = globalDefault.getCatalog();
				if ( globalDefault.getDelimitedIdentifier() != null ) delimitedIdentifier = globalDefault.getDelimitedIdentifier();
				if ( globalDefault.getMetadataComplete() != null ) {
					metadataComplete = globalDefault.getMetadataComplete();
				}
				//TODO fix that in stone if cascade-persist is set already?
				if ( globalDefault.getCascadePersist() != null ) cascadePersist = globalDefault.getCascadePersist();
			}
		}

		public void setDelimitedIdentifiers(Boolean delimitedIdentifier) {
			this.delimitedIdentifier = delimitedIdentifier;
		}

		public Boolean getDelimitedIdentifier() {
			return delimitedIdentifier;
		}
	}

	public List<String> getDefaultEntityListeners() {
		return defaultEntityListeners;
	}
}
