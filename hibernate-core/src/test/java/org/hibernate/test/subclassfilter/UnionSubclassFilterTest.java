/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.subclassfilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class UnionSubclassFilterTest extends BaseCoreFunctionalTestCase {
	public final String[] getMappings() {
		return new String[] { "subclassfilter/union-subclass.hbm.xml" };
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	@FailureExpectedWithNewMetamodel
	public void testFiltersWithUnionSubclass() {
		Session s = openSession();
		s.enableFilter( "region" ).setParameter( "userRegion", "US" );
		Transaction t = s.beginTransaction();

		prepareTestData( s );
		s.clear();

		List results;
		Iterator itr;

		results = s.createQuery( "from Person" ).list();
		assertEquals( "Incorrect qry result count", 4, results.size() );
		s.clear();

		results = s.createQuery( "from Employee" ).list();
		assertEquals( "Incorrect qry result count", 2, results.size() );
		s.clear();

		results = new ArrayList( new HashSet( s.createQuery( "from Person as p left join fetch p.minions" ).list() ) );
		assertEquals( "Incorrect qry result count", 4, results.size() );
		itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 1, john.getMinions().size() );
				break;
			}
		}
		s.clear();

		results = new ArrayList( new HashSet( s.createQuery( "from Employee as p left join fetch p.minions" ).list() ) );
		assertEquals( "Incorrect qry result count", 2, results.size() );
		itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 1, john.getMinions().size() );
				break;
			}
		}

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		for ( Object entity : s.createQuery( "from Person" ).list() ) {
			s.delete( entity );
		}
		t.commit();
		s.close();

	}

	@SuppressWarnings( {"unchecked"})
	private void prepareTestData(Session s) {
		Employee john = new Employee( "John Doe" );
		john.setCompany( "JBoss" );
		john.setDepartment( "hr" );
		john.setTitle( "hr guru" );
		john.setRegion( "US" );

		Employee polli = new Employee( "Polli Wog" );
		polli.setCompany( "JBoss" );
		polli.setDepartment( "hr" );
		polli.setTitle( "hr novice" );
		polli.setRegion( "US" );
		polli.setManager( john );
		john.getMinions().add( polli );

		Employee suzie = new Employee( "Suzie Q" );
		suzie.setCompany( "JBoss" );
		suzie.setDepartment( "hr" );
		suzie.setTitle( "hr novice" );
		suzie.setRegion( "EMEA" );
		suzie.setManager( john );
		john.getMinions().add( suzie );

		Customer cust = new Customer( "John Q Public" );
		cust.setCompany( "Acme" );
		cust.setRegion( "US" );
		cust.setContactOwner( john );

		Person ups = new Person( "UPS guy" );
		ups.setCompany( "UPS" );
		ups.setRegion( "US" );

		s.save( john );
		s.save( cust );
		s.save( ups );

		s.flush();
	}
}
