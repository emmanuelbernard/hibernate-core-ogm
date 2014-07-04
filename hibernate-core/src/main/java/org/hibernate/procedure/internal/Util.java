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
package org.hibernate.procedure.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.UnknownSqlResultSetMappingException;

import org.jboss.logging.Logger;

/**
 * Utilities used to implement procedure call support.
 *
 * @author Steve Ebersole
 */
public class Util {
	private static final Logger log = Logger.getLogger( Util.class );

	private Util() {
	}

	/**
	 * Makes a copy of the given query return array.
	 *
	 * @param queryReturns The returns to copy
	 *
	 * @return The copy
	 */
	public static NativeSQLQueryReturn[] copy(NativeSQLQueryReturn[] queryReturns) {
		if ( queryReturns == null ) {
			return new NativeSQLQueryReturn[0];
		}

		final NativeSQLQueryReturn[] copy = new NativeSQLQueryReturn[ queryReturns.length ];
		System.arraycopy( queryReturns, 0, copy, 0, queryReturns.length );
		return copy;
	}

	/**
	 * Make a (shallow) copy of query spaces to be synchronized
	 *
	 * @param synchronizedQuerySpaces The query spaces
	 *
	 * @return The copy
	 */
	public static Set<String> copy(Set<String> synchronizedQuerySpaces) {
		return CollectionHelper.makeCopy( synchronizedQuerySpaces );
	}

	/**
	 * Make a (shallow) copy of the JPA query hints map
	 *
	 * @param hints The JPA query hints to copy
	 *
	 * @return The copy
	 */
	public static Map<String,Object> copy(Map<String, Object> hints) {
		return CollectionHelper.makeCopy( hints );
	}

	/**
	 * Context for resolving result-set-mapping definitions
	 */
	public static interface ResultSetMappingResolutionContext {
		/**
		 * Access to the SessionFactory
		 *
		 * @return SessionFactory
		 */
		public SessionFactoryImplementor getSessionFactory();

		/**
		 * Locate a ResultSetMappingDefinition by name
		 *
		 * @param name The name of the ResultSetMappingDefinition to locate
		 *
		 * @return The ResultSetMappingDefinition
		 */
		public ResultSetMappingDefinition findResultSetMapping(String name);

		/**
		 * Callback to add query returns indicated by the result set mapping(s)
		 *
		 * @param queryReturns The query returns
		 */
		public void addQueryReturns(NativeSQLQueryReturn... queryReturns);

		/**
		 * Callback to add query spaces indicated by the result set mapping(s)
		 *
		 * @param querySpaces The query spaces
		 */
		public void addQuerySpaces(String... querySpaces);
	}

	/**
	 * Resolve the given result set mapping names
	 *
	 * @param context The context for the resolution.  See {@link ResultSetMappingResolutionContext}
	 * @param resultSetMappingNames The names of the result-set-mappings to resolve
	 */
	public static void resolveResultSetMappings(ResultSetMappingResolutionContext context, String... resultSetMappingNames) {
		for ( String resultSetMappingName : resultSetMappingNames ) {
			log.tracef( "Starting attempt resolve named result-set-mapping : %s", resultSetMappingName );
			final ResultSetMappingDefinition mapping = context.findResultSetMapping( resultSetMappingName );
			if ( mapping == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}

			log.tracef( "Found result-set-mapping : %s", mapping.traceLoggableFormat() );

			context.addQueryReturns( mapping.getQueryReturns() );

			final SQLQueryReturnProcessor processor =
					new SQLQueryReturnProcessor( mapping.getQueryReturns(), context.getSessionFactory() );
			final SQLQueryReturnProcessor.ResultAliasContext processResult = processor.process();
			context.addQuerySpaces( processResult.collectQuerySpaces() );
		}
	}

	/**
	 * Resolve the given result set mapping names
	 *
	 * @param context The context for the resolution.  See {@link ResultSetMappingResolutionContext}
	 * @param resultSetMappingNames The names of the result-set-mappings to resolve
	 */
	public static void resolveResultSetMappings(ResultSetMappingResolutionContext context, Collection<String> resultSetMappingNames) {
		for ( String resultSetMappingName : resultSetMappingNames ) {
			log.tracef( "Starting attempt resolve named result-set-mapping : %s", resultSetMappingName );
			final ResultSetMappingDefinition mapping = context.findResultSetMapping( resultSetMappingName );
			if ( mapping == null ) {
				throw new UnknownSqlResultSetMappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}

			log.tracef( "Found result-set-mapping : %s", mapping.traceLoggableFormat() );

			context.addQueryReturns( mapping.getQueryReturns() );

			final SQLQueryReturnProcessor processor =
					new SQLQueryReturnProcessor( mapping.getQueryReturns(), context.getSessionFactory() );
			final SQLQueryReturnProcessor.ResultAliasContext processResult = processor.process();
			context.addQuerySpaces( processResult.collectQuerySpaces() );
		}
	}

	/**
	 * Context for resolving result-class definitions
	 */
	public static interface ResultClassesResolutionContext {
		/**
		 * Access to the SessionFactory
		 *
		 * @return SessionFactory
		 */
		public SessionFactoryImplementor getSessionFactory();
		/**
		 * Callback to add query returns indicated by the result set mapping(s)
		 *
		 * @param queryReturns The query returns
		 */
		public void addQueryReturns(NativeSQLQueryReturn... queryReturns);

		/**
		 * Callback to add query spaces indicated by the result set mapping(s)
		 *
		 * @param querySpaces The query spaces
		 */
		public void addQuerySpaces(String... querySpaces);
	}

	/**
	 * Resolve the given result classes
	 *
	 * @param context The context for the resolution.  See {@link ResultSetMappingResolutionContext}
	 * @param resultClasses The Classes to which the results should be mapped
	 */
	public static void resolveResultClasses(ResultClassesResolutionContext context, Class... resultClasses) {
		int i = 0;
		for ( Class resultClass : resultClasses ) {
			context.addQueryReturns(
					new NativeSQLQueryRootReturn( "alias" + (++i), resultClass.getName(), LockMode.READ )
			);
			try {
				final EntityPersister persister = context.getSessionFactory().getEntityPersister( resultClass.getName() );
				context.addQuerySpaces( (String[]) persister.getQuerySpaces() );
			}
			catch (Exception ignore) {
			}
		}
	}
}
