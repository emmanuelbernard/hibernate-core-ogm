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

import javassist.util.proxy.ProxyFactory;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class EntityTools {
	public static boolean entitiesEqual(SessionImplementor session, String entityName, Object obj1, Object obj2) {
		final Object id1 = getIdentifier( session, entityName, obj1 );
		final Object id2 = getIdentifier( session, entityName, obj2 );

		return Tools.objectsEqual( id1, id2 );
	}

	public static Object getIdentifier(SessionImplementor session, String entityName, Object obj) {
		if ( obj == null ) {
			return null;
		}

		if ( obj instanceof HibernateProxy ) {
			final HibernateProxy hibernateProxy = (HibernateProxy) obj;
			return hibernateProxy.getHibernateLazyInitializer().getIdentifier();
		}

		return session.getEntityPersister( entityName, obj ).getIdentifier( obj, session );
	}

	public static Object getTargetFromProxy(SessionFactoryImplementor sessionFactoryImplementor, HibernateProxy proxy) {
		if ( !proxy.getHibernateLazyInitializer().isUninitialized() || activeProxySession( proxy ) ) {
			return proxy.getHibernateLazyInitializer().getImplementation();
		}

		final SessionImplementor sessionImplementor = proxy.getHibernateLazyInitializer().getSession();
		final Session tempSession = sessionImplementor == null
				? sessionFactoryImplementor.openTemporarySession()
				: sessionImplementor.getFactory().openTemporarySession();
		try {
			return tempSession.get(
					proxy.getHibernateLazyInitializer().getEntityName(),
					proxy.getHibernateLazyInitializer().getIdentifier()
			);
		}
		finally {
			tempSession.close();
		}
	}

	private static boolean activeProxySession(HibernateProxy proxy) {
		final Session session = (Session) proxy.getHibernateLazyInitializer().getSession();
		return session != null && session.isOpen() && session.isConnected();
	}

	/**
	 * @param clazz Class wrapped with a proxy or not.
	 * @param <T> Class type.
	 *
	 * @return Returns target class in case it has been wrapped with a proxy. If {@code null} reference is passed,
	 *         method returns {@code null}.
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> Class<T> getTargetClassIfProxied(Class<T> clazz) {
		if ( clazz == null ) {
			return null;
		}
		else if ( ProxyFactory.isProxyClass( clazz ) ) {
			// Get the source class of Javassist proxy instance.
			return (Class<T>) clazz.getSuperclass();
		}
		return clazz;
	}

	/**
	 * @return Java class mapped to specified entity name.
	 */
	public static Class getEntityClass(SessionImplementor sessionImplementor, Session session, String entityName) {
		final EntityPersister entityPersister = sessionImplementor.getFactory().getEntityPersister( entityName );
		return entityPersister.getMappedClass();
	}
}
