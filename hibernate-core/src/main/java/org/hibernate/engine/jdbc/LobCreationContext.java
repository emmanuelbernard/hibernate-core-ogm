/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides callback access into the context in which the LOB is to be created.
 *
 * @author Steve Ebersole
 */
public interface LobCreationContext {
	/**
	 * The callback contract for making use of the JDBC {@link Connection}.
	 */
	public static interface Callback<T> {
		/**
		 * Perform whatever actions are necessary using the provided JDBC {@link Connection}.
		 *
		 * @param connection The JDBC {@link Connection}.
		 *
		 * @return The created LOB.
		 *
		 * @throws SQLException Indicates trouble accessing the JDBC driver to create the LOB
		 */
		public T executeOnConnection(Connection connection) throws SQLException;
	}

	/**
	 * Execute the given callback, making sure it has access to a viable JDBC {@link Connection}.
	 *
	 * @param callback The callback to execute .
	 * @param <T> The Java type of the type of LOB being created.  One of {@link java.sql.Blob},
	 * {@link java.sql.Clob}, {@link java.sql.NClob}
	 *
	 * @return The LOB created by the callback.
	 */
	public <T> T execute(Callback<T> callback);
}
