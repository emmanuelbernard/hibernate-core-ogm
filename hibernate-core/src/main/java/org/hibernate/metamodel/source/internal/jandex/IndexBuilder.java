/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.jandex.filter.IndexedAnnotationFilter;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;

/**
 * @author Strong Liu
 */
public class IndexBuilder {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IndexBuilder.class );

	private IndexView index;
	private final ServiceRegistry serviceRegistry;

	private final Map<DotName, List<AnnotationInstance>> annotations;
	private final Map<DotName, List<ClassInfo>> subclasses;
	private final Map<DotName, List<ClassInfo>> implementors;
	private final Map<DotName, ClassInfo> classes;
	private final Map<DotName, Map<DotName, List<AnnotationInstance>>> classInfoAnnotationsMap;
	private final Map<DotName, Map<DotName, List<AnnotationInstance>>> indexedClassInfoAnnotationsMap;

	public IndexBuilder(IndexView index, ServiceRegistry serviceRegistry) {
		this.index = index;
		this.serviceRegistry = serviceRegistry;
		this.annotations = new HashMap<DotName, List<AnnotationInstance>>();
		this.subclasses = new HashMap<DotName, List<ClassInfo>>();
		this.implementors = new HashMap<DotName, List<ClassInfo>>();
		this.classes = new HashMap<DotName, ClassInfo>();
		this.classInfoAnnotationsMap = new HashMap<DotName, Map<DotName, List<AnnotationInstance>>>();
		this.indexedClassInfoAnnotationsMap = new HashMap<DotName, Map<DotName, List<AnnotationInstance>>>();
	}

	/**
	 * Build new {@link Index} with mocked annotations from orm.xml.
	 * This method should be only called once per {@link IndexBuilder} instance.
	 *
	 * @param globalDefaults Global defaults from <persistence-unit-metadata>, or null.
	 *
	 * @return Index.
	 */
	public Index build(Default globalDefaults) {
		//merge annotations that not overrided by xml into the new Index
		for ( ClassInfo ci : index.getKnownClasses() ) {
			DotName name = ci.name();
			if ( indexedClassInfoAnnotationsMap.containsKey( name ) ) {
				//this class has been overrided by orm.xml
				continue;
			}
			ClassInfo mockedClassInfo = createClassInfo( name.toString() );
			if ( CollectionHelper.isNotEmpty( ci.annotations() ) ) {
				Map<DotName, List<AnnotationInstance>> tmp = new HashMap<DotName, List<AnnotationInstance>>( ci.annotations() );
				DefaultConfigurationHelper.INSTANCE.applyDefaults( tmp, globalDefaults );
				mergeAnnotationMap( tmp, annotations );
				classInfoAnnotationsMap.get( name ).putAll( tmp );
//				classes.put( name, ci );
//				if ( ci.superName() != null ) {
//					addSubClasses( ci.superName(), ci );
//				}
//				if ( CollectionHelper.isNotEmpty( ci.interfaces() ) ) {
//					addImplementors( ci.interfaces(), ci );
//				}
			}
		}
		return Index.create(
				annotations, subclasses, implementors, classes
		);
	}
	Map<DotName, List<AnnotationInstance>> getAnnotations(){
		return Collections.unmodifiableMap( annotations );
	}
	/**
	 * If {@code xml-mapping-metadata-complete} is defined in PersistenceUnitMetadata, we create a new empty {@link Index} here.
	 */
	void mappingMetadataComplete() {
		LOG.debug(
				"xml-mapping-metadata-complete is specified in persistence-unit-metadata, ignore JPA annotations."
		);
		index = Index.create(
				new HashMap<DotName, List<AnnotationInstance>>(),
				new HashMap<DotName, List<ClassInfo>>(),
				new HashMap<DotName, List<ClassInfo>>(),
				new HashMap<DotName, ClassInfo>()
		);
	}

	/**
	 * @param name Entity Object dot name which is being process.
	 */
	void metadataComplete(DotName name) {
		LOG.debug(
				"metadata-complete is specified in " + name + ", ignore JPA annotations."
		);
		getIndexedAnnotations( name ).clear();
	}

	public Map<DotName, List<AnnotationInstance>> getIndexedAnnotations(DotName name) {
		Map<DotName, List<AnnotationInstance>> map = indexedClassInfoAnnotationsMap.get( name );
		if ( map == null ) {
			ClassInfo ci = index.getClassByName( name );
			if ( ci == null || ci.annotations() == null ) {
				map = Collections.emptyMap();
			}
			else {
				map = new HashMap<DotName, List<AnnotationInstance>>( ci.annotations() );
				//here we ignore global annotations
				for ( DotName globalAnnotationName : DefaultConfigurationHelper.GLOBAL_ANNOTATIONS ) {
					if ( map.containsKey( globalAnnotationName ) ) {
						map.put( globalAnnotationName, Collections.<AnnotationInstance>emptyList() );
					}
				}
			}
			indexedClassInfoAnnotationsMap.put( name, map );
		}
		return map;
	}

	public Map<DotName, List<AnnotationInstance>> getClassInfoAnnotationsMap(DotName name) {
		return classInfoAnnotationsMap.get( name );
	}

	public ClassInfo getClassInfo(DotName name) {
		return classes.get( name );
	}

	public ClassInfo getIndexedClassInfo(DotName name) {
		return index.getClassByName( name );
	}

	void collectGlobalConfigurationFromIndex(GlobalAnnotations globalAnnotations) {
		for ( DotName annName : DefaultConfigurationHelper.GLOBAL_ANNOTATIONS ) {
			Collection<AnnotationInstance> annotationInstances = index.getAnnotations( annName );
			if ( CollectionHelper.isNotEmpty( annotationInstances ) ) {
				globalAnnotations.addIndexedAnnotationInstance( annotationInstances );
			}
		}
		globalAnnotations.filterIndexedAnnotations();
	}

	void finishGlobalConfigurationMocking(GlobalAnnotations globalAnnotations) {
		annotations.putAll( globalAnnotations.getAnnotationInstanceMap() );
	}

	void finishEntityObject(final DotName name, final Default defaults) {
		Map<DotName, List<AnnotationInstance>> map = classInfoAnnotationsMap.get( name );
		if ( map == null ) {
			throw new AssertionFailure( "Calling finish entity object " + name + " before create it." );
		}
		// annotations classes overrided by xml
		if ( indexedClassInfoAnnotationsMap.containsKey( name ) ) {
			Map<DotName, List<AnnotationInstance>> tmp = getIndexedAnnotations( name );
			mergeAnnotationMap( tmp, map );
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults( map, defaults );
		
		mergeAnnotationMap( map, annotations );
	}


	void addAnnotationInstance(DotName targetClassName, AnnotationInstance annotationInstance) {
		if ( annotationInstance == null ) {
			return;
		}
		for ( IndexedAnnotationFilter indexedAnnotationFilter : IndexedAnnotationFilter.ALL_FILTERS ) {
			indexedAnnotationFilter.beforePush( this, targetClassName, annotationInstance );
		}
		Map<DotName, List<AnnotationInstance>> map = classInfoAnnotationsMap.get( targetClassName );
		if ( map == null ) {
			throw new AssertionFailure( "Can't find " + targetClassName + " in internal cache, should call createClassInfo first" );
		}

		List<AnnotationInstance> annotationInstanceList = map.get( annotationInstance.name() );
		if ( annotationInstanceList == null ) {
			annotationInstanceList = new ArrayList<AnnotationInstance>();
			map.put( annotationInstance.name(), annotationInstanceList );
		}
		annotationInstanceList.add( annotationInstance );
	}

	ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	ClassInfo createClassInfo(String className, String optionalPerfix){
		if ( StringHelper.isEmpty( className ) ) {
			throw new AssertionFailure( "Class Name used to create ClassInfo is empty." );
		}

		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		Class clazz = null;
		try {
			clazz = classLoaderService.classForName( className );
		}
		catch ( ClassLoadingException e ) {
			if ( StringHelper.isNotEmpty( optionalPerfix ) ) {
				className = StringHelper.qualify( optionalPerfix, className );
				clazz = classLoaderService.classForName( className );
			}
		}
		DotName classDotName = DotName.createSimple( className );
		if ( classes.containsKey( classDotName ) ) {
			//classInfoAnnotationsMap.put( classDotName, new HashMap<DotName, List<AnnotationInstance>>(classes.get( classDotName ).annotations()) );
			return classes.get( classDotName );
		}

		DotName superName = null;
		DotName[] interfaces = null;
		short access_flag;
		ClassInfo annClassInfo = index.getClassByName( classDotName );
		if ( annClassInfo != null ) {
			superName = annClassInfo.superName();
			interfaces = annClassInfo.interfaces();
			access_flag = annClassInfo.flags();
		}
		else {
			Class superClass = clazz.getSuperclass();
			if ( superClass != null ) {
				superName = DotName.createSimple( superClass.getName() );
			}
			Class[] classInterfaces = clazz.getInterfaces();
			if ( classInterfaces != null && classInterfaces.length > 0 ) {
				interfaces = new DotName[classInterfaces.length];
				for ( int i = 0; i < classInterfaces.length; i++ ) {
					interfaces[i] = DotName.createSimple( classInterfaces[i].getName() );
				}
			}
			access_flag = (short) ( clazz.getModifiers() | 0x20 );//(modifiers | ACC_SUPER)
		}
		Map<DotName, List<AnnotationInstance>> map = new HashMap<DotName, List<AnnotationInstance>>();
		classInfoAnnotationsMap.put( classDotName, map );
		ClassInfo classInfo = ClassInfo.create(
				classDotName, superName, access_flag, interfaces, map
		);
		classes.put( classDotName, classInfo );
		addSubClasses( superName, classInfo );
		addImplementors( interfaces, classInfo );
		return classInfo;
	}

	ClassInfo createClassInfo(String className) {
		return createClassInfo( className, null );
	}

	private void addSubClasses(DotName superClassDotName, ClassInfo classInfo) {
		if ( superClassDotName != null ) {
			List<ClassInfo> classInfoList = subclasses.get( superClassDotName );
			if ( classInfoList == null ) {
				classInfoList = new ArrayList<ClassInfo>();
				subclasses.put( superClassDotName, classInfoList );
			}
			classInfoList.add( classInfo );
		}
	}

	private void addImplementors(DotName[] dotNames, ClassInfo classInfo) {
		if ( dotNames != null && dotNames.length > 0 ) {
			for ( DotName dotName : dotNames ) {
				List<ClassInfo> classInfoList = implementors.get( dotName );
				if ( classInfoList == null ) {
					classInfoList = new ArrayList<ClassInfo>();
					implementors.put( dotName, classInfoList );
				}
				classInfoList.add( classInfo );
			}
		}
	}

	//merge source into target
	private void mergeAnnotationMap(Map<DotName, List<AnnotationInstance>> source, Map<DotName, List<AnnotationInstance>> target) {
		if ( source != null ) {
			for ( Map.Entry<DotName, List<AnnotationInstance>> el : source.entrySet() ) {
				if ( el.getValue().isEmpty() ) {
					continue;
				}
				DotName annotationName = el.getKey();
				List<AnnotationInstance> value = el.getValue();
				List<AnnotationInstance> annotationInstanceList = target.get( annotationName );
				if ( annotationInstanceList == null ) {
					annotationInstanceList = new ArrayList<AnnotationInstance>();
					target.put( annotationName, annotationInstanceList );
				}
				annotationInstanceList.addAll( value );
			}
		}
	}
}
