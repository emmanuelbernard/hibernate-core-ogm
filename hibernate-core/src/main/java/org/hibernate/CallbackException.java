/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

/**
 * Intended to be thrown from {@link org.hibernate.classic.Lifecycle} and {@link Interceptor} callbacks.
 * <p/>
 * IMPL NOTE : This is a legacy exception type from back in the day before Hibernate moved to a untyped (runtime)
 * exception strategy.
 *
 * @author Gavin King
 */
public class CallbackException extends HibernateException {
	/**
	 * Creates a CallbackException using the given underlying cause.
	 *
	 * @param cause The underlying cause
	 */
	public CallbackException(Exception cause) {
		this( "An exception occurred in a callback", cause );
	}

	/**
	 * Creates a CallbackException using the given message.
	 *
	 * @param message The message explaining the reason for the exception
	 */
	public CallbackException(String message) {
		super( message );
	}

	/**
	 * Creates a CallbackException using the given message and underlying cause.
	 *
	 * @param message The message explaining the reason for the exception
	 * @param cause The underlying cause
	 */
	public CallbackException(String message, Exception cause) {
		super( message, cause );
	}

}
