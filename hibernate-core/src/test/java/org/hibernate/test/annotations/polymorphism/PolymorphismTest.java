/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.polymorphism;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class PolymorphismTest extends BaseCoreFunctionalTestCase {
	
	@Test
	public void testPolymorphism() throws Exception {
		Car car = new Car();
		car.setModel( "SUV" );
		SportCar car2 = new SportCar();
		car2.setModel( "350Z" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( car );
		s.persist( car2 );
		s.flush();
		assertEquals( 2, s.createQuery( "select car from Car car").list().size() );
		assertEquals( 0, s.createQuery( "select count(m) from " + Automobile.class.getName() + " m").list().size() );
		tx.rollback();
		s.close();

	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-7915")
	public void testNonPkInheritedFk() throws Exception {
		MarketRegion region1 = new MarketRegion();
		region1.setRegionCode( "US" );
		MarketRegion region2 = new MarketRegion();
		region2.setRegionCode( "EU" );
		
		Car car = new Car();
		car.setModel( "SUV" );
		car.setMarketRegion( region1 );
		
		SportCar car2 = new SportCar();
		car2.setModel( "350Z" );
		car2.setMarketRegion( region2 );
		
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( region1 );
		s.persist( region2 );
		s.persist( car );
		s.persist( car2 );
		
		s.flush();
		
		assertEquals( 1, s.createQuery( "select car from Car car where car.marketRegion.regionCode='US'")
				.list().size() );
		assertEquals( 1, s.createQuery( "select car from SportCar car where car.marketRegion.regionCode='EU'")
				.list().size() );
		
		tx.rollback();
		s.close();

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				SportCar.class,
				MarketRegion.class
		};
	}
}
