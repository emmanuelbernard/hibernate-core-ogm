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
package org.hibernate.test.loadplans.process;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class SimpleResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Test
	public void testSimpleEntityProcessing() throws Exception {
		final EntityPersister entityPersister = sessionFactory().getEntityPersister( SimpleEntity.class.getName() );

		// create some test data
		Session session = openSession();
		session.beginTransaction();
		session.save( new SimpleEntity( 1, "the only" ) );
		session.getTransaction().commit();
		session.close();

		{
			final LoadPlan plan = Helper.INSTANCE.buildLoadPlan( sessionFactory(), entityPersister );

			final LoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( plan, sessionFactory() );
			final String sql = queryDetails.getSqlStatement();
			final ResultSetProcessor resultSetProcessor = queryDetails.getResultSetProcessor();

			final List results = new ArrayList();

			final Session workSession = openSession();
			workSession.beginTransaction();
			workSession.doWork(
					new Work() {
						@Override
						public void execute(Connection connection) throws SQLException {
							( (SessionImplementor) workSession ).getFactory().getJdbcServices().getSqlStatementLogger().logStatement( sql );
							PreparedStatement ps = connection.prepareStatement( sql );
							ps.setInt( 1, 1 );
							ResultSet resultSet = ps.executeQuery();
							results.addAll(
									resultSetProcessor.extractResults(
											resultSet,
											(SessionImplementor) workSession,
											new QueryParameters(),
											new NamedParameterContext() {
												@Override
												public int[] getNamedParameterLocations(String name) {
													return new int[0];
												}
											},
											true,
											false,
											null,
											null
									)
							);
							resultSet.close();
							ps.close();
						}
					}
			);
			assertEquals( 1, results.size() );
			Object result = results.get( 0 );
			assertNotNull( result );

			SimpleEntity workEntity = ExtraAssertions.assertTyping( SimpleEntity.class, result );
			assertEquals( 1, workEntity.id.intValue() );
			assertEquals( "the only", workEntity.name );
			workSession.getTransaction().commit();
			workSession.close();
		}

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete SimpleEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id public Integer id;
		public String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
