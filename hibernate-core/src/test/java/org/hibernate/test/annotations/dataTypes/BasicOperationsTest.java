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
package org.hibernate.test.annotations.dataTypes;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(value = {DialectChecks.SupportsExpectedLobUsagePattern.class, BasicOperationsTest.OracleDialectChecker.class}, jiraKey = "HHH-6834")
public class BasicOperationsTest extends BaseCoreFunctionalTestCase {

	private static final String SOME_ENTITY_TABLE_NAME = "SOMEENTITY";
	private static final String SOME_OTHER_ENTITY_TABLE_NAME = "SOMEOTHERENTITY";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SomeEntity.class, SomeOtherEntity.class };
	}
	public static class OracleDialectChecker implements DialectCheck{
		@Override
		public boolean isMatch(Dialect dialect) {
			return ! (dialect instanceof Oracle8iDialect);
		}
	}

	@Test
	public void testCreateAndDelete() {
		Date now = new Date();

		Session s = openSession();

		s.doWork( new ValidateSomeEntityColumns( (SessionImplementor) s ) );
		s.doWork( new ValidateRowCount( (SessionImplementor) s, SOME_ENTITY_TABLE_NAME, 0 ) );
		s.doWork( new ValidateRowCount( (SessionImplementor) s, SOME_OTHER_ENTITY_TABLE_NAME, 0 ) );

		s.beginTransaction();
		SomeEntity someEntity = new SomeEntity( now );
		SomeOtherEntity someOtherEntity = new SomeOtherEntity( 1 );
		s.save( someEntity );
		s.save( someOtherEntity );
		s.getTransaction().commit();
		s.close();

		s = openSession();

		s.doWork( new ValidateRowCount( (SessionImplementor) s, SOME_ENTITY_TABLE_NAME, 1 ) );
		s.doWork( new ValidateRowCount( (SessionImplementor) s, SOME_OTHER_ENTITY_TABLE_NAME, 1 ) );

		s.beginTransaction();
		s.delete( someEntity );
		s.delete( someOtherEntity );
		s.getTransaction().commit();

		s.doWork( new ValidateRowCount( (SessionImplementor) s, SOME_ENTITY_TABLE_NAME, 0 ) );
		s.doWork( new ValidateRowCount( (SessionImplementor) s, SOME_OTHER_ENTITY_TABLE_NAME, 0 ) );

		s.close();
	}

	// verify all the expected columns are created
	class ValidateSomeEntityColumns implements Work {
		private SessionImplementor s;
		
		public ValidateSomeEntityColumns( SessionImplementor s ) {
			this.s = s;
		}
		
		public void execute(Connection connection) throws SQLException {
			// id -> java.util.Date (DATE - becase of explicit TemporalType)
			validateColumn( connection, "ID", java.sql.Types.DATE );

			// timeData -> java.sql.Time (TIME)
			validateColumn( connection, "TIMEDATA", java.sql.Types.TIME );

			// tsData -> java.sql.Timestamp (TIMESTAMP)
			validateColumn( connection, "TSDATA", java.sql.Types.TIMESTAMP );
		}

		private void validateColumn(Connection connection, String columnName, int expectedJdbcTypeCode)
				throws SQLException {
			DatabaseMetaData meta = connection.getMetaData();

			// DBs treat the meta information differently, in particular case sensitivity.
			// We need to use the meta information to find out how to treat names
			String tableNamePattern = generateFinalNamePattern( meta, SOME_ENTITY_TABLE_NAME );
			String columnNamePattern = generateFinalNamePattern( meta, columnName );

			ResultSet columnInfo = meta.getColumns( null, null, tableNamePattern, columnNamePattern );
			s.getTransactionCoordinator().getJdbcCoordinator().register(columnInfo, columnInfo.getStatement());
			assertTrue( columnInfo.next() );
			int dataType = columnInfo.getInt( "DATA_TYPE" );
			s.getTransactionCoordinator().getJdbcCoordinator().release( columnInfo, columnInfo.getStatement() );
			assertEquals(
					columnName,
					JdbcTypeNameMapper.getTypeName( expectedJdbcTypeCode ),
					JdbcTypeNameMapper.getTypeName( dataType )
			);
		}

		private String generateFinalNamePattern(DatabaseMetaData meta, String name) throws SQLException {
			if ( meta.storesLowerCaseIdentifiers() ) {
				return name.toLowerCase();
			}
			else {
				return name;
			}
		}
	}

	// verify we have the right amount of columns
	class ValidateRowCount implements Work {
		private final int expectedRowCount;
		private final String table;

		private SessionImplementor s;
		
		public ValidateRowCount(SessionImplementor s, String table, int count) {
			this.s = s;
			this.expectedRowCount = count;
			this.table = table;
		}

		public void execute(Connection connection) throws SQLException {
			Statement st = s.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().createStatement();
			s.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st, "SELECT COUNT(*) FROM " + table );
			ResultSet result = s.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st, "SELECT COUNT(*) FROM " + table );
			result.next();
			int rowCount = result.getInt( 1 );
			assertEquals( "Unexpected row count", expectedRowCount, rowCount );
		}
	}
}

