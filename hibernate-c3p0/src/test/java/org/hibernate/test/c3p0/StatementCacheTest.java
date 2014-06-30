/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.c3p0;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that when using cached prepared statement with batching enabled doesn't bleed over into new transactions. 
 * 
 * @author Shawn Clowater
 */
public class StatementCacheTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-7193" )
	public void testStatementCaching() {
		Session session = openSession();
		session.beginTransaction();

		//save 2 new entities, one valid, one invalid (neither should be persisted)
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "valid 1" );
		session.save( irrelevantEntity );
		//name is required
		irrelevantEntity = new IrrelevantEntity();
		session.save( irrelevantEntity );
		try {
			session.flush();
			Assert.fail( "Validation exception did not occur" );
		}
		catch (Exception e) {
			//this is expected roll the transaction back
			session.getTransaction().rollback();
		}
		session.close();

		session = openSession();
		session.beginTransaction();

		//save a new entity and commit it
		irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "valid 2" );
		session.save( irrelevantEntity );
		session.flush();
		session.getTransaction().commit();
		session.close();

		//only one entity should have been inserted to the database (if the statement in the cache wasn't cleared then it would have inserted both entities)
		session = openSession();
		session.beginTransaction();
		Criteria criteria = session.createCriteria( IrrelevantEntity.class );
		List results = criteria.list();
		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 1, results.size() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ IrrelevantEntity.class };
	}
}
