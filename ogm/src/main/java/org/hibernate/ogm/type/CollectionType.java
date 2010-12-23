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
package org.hibernate.ogm.type;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Emmanuel Bernard
 */
public class CollectionType extends GridTypeDelegatingToCoreType {
	private final org.hibernate.type.CollectionType delegate;

	CollectionType(org.hibernate.type.CollectionType type) {
		super( type );
		this.delegate = type;
	}

	@Override
	public Object nullSafeGet(Map<String, Object> rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return resolve( null, session, owner );
	}

	@Override
	public Object nullSafeGet(Map<String, Object> rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Map<String, Object> resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		//NOOP
	}

	@Override
	public void nullSafeSet(Map<String, Object> resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		//NOOP
	}

	@Override
	public Object hydrate(Map<String, Object> rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		//CollectionType.delegate returns a marker object. We pass it through.
		return delegate.hydrate( null, names, session, owner );
	}
}
