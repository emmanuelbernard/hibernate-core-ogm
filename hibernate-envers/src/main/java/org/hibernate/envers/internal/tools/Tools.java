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
package org.hibernate.envers.internal.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AssertionFailure;
import org.hibernate.envers.tools.Pair;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class Tools {
	public static <K, V> Map<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <E> Set<E> newHashSet() {
		return new HashSet<E>();
	}

	public static <K, V> Map<K, V> newLinkedHashMap() {
		return new LinkedHashMap<K, V>();
	}

	public static boolean objectsEqual(Object obj1, Object obj2) {
		if ( obj1 == null ) {
			return obj2 == null;
		}

		return obj1.equals( obj2 );
	}

	public static <T> List<T> iteratorToList(Iterator<T> iter) {
		final List<T> ret = new ArrayList<T>();
		while ( iter.hasNext() ) {
			ret.add( iter.next() );
		}

		return ret;
	}

	public static boolean iteratorsContentEqual(Iterator iter1, Iterator iter2) {
		while ( iter1.hasNext() && iter2.hasNext() ) {
			if ( !iter1.next().equals( iter2.next() ) ) {
				return false;
			}
		}

		//noinspection RedundantIfStatement
		if ( iter1.hasNext() || iter2.hasNext() ) {
			return false;
		}

		return true;
	}

	/**
	 * Transforms a list of arbitrary elements to a list of index-element pairs.
	 *
	 * @param list List to transform.
	 *
	 * @return A list of pairs: ((0, element_at_index_0), (1, element_at_index_1), ...)
	 */
	public static <T> List<Pair<Integer, T>> listToIndexElementPairList(List<T> list) {
		final List<Pair<Integer, T>> ret = new ArrayList<Pair<Integer, T>>();
		final Iterator<T> listIter = list.iterator();
		for ( int i = 0; i < list.size(); i++ ) {
			ret.add( Pair.make( i, listIter.next() ) );
		}

		return ret;
	}

	public static boolean isFieldOrPropertyOfClass(AnnotationTarget target, ClassInfo clazz, IndexView jandexIndex) {
		final ClassInfo enclosingClass;
		if ( target instanceof FieldInfo ) {
			final FieldInfo field = (FieldInfo) target;
			enclosingClass = field.declaringClass();
		}
		else if ( target instanceof MethodInfo ) {
			final MethodInfo method = (MethodInfo) target;
			enclosingClass = method.declaringClass();
		}
		else {
			throw new AssertionFailure( "Unexpected annotation target " + target.toString() );
		}
		return enclosingClass.equals( clazz ) || jandexIndex.getAllKnownSubclasses( enclosingClass.name() ).contains( clazz );
	}

}
