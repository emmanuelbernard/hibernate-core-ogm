/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.queryhint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@RequiresDialect( Oracle8iDialect.class )
public class QueryHintTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, Department.class };
	}
	
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DIALECT, QueryHintTestDialect.class.getName() );
		configuration.setProperty( AvailableSettings.USE_SQL_COMMENTS, "true" );
	}
	
	@Test
	public void testQueryHint() {
		Department department = new Department();
		department.name = "Sales";
		Employee employee1 = new Employee();
		employee1.department = department;
		Employee employee2 = new Employee();
		employee2.department = department;
		
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( department );		s.persist( employee1 );
		s.persist( employee2 );
		s.getTransaction().commit();
		s.clear();

		// test Query w/ a simple Oracle optimizer hint
		s.getTransaction().begin();
		Query query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
				.addQueryHint( "ALL_ROWS" )
				.setParameter( "departmentName", "Sales" );
		List results = query.list();
		s.getTransaction().commit();
		s.clear();
		
		assertEquals(results.size(), 2);
		assertTrue(QueryHintTestDialect.getProcessedSql().contains( "select /*+ ALL_ROWS */"));
		
		QueryHintTestDialect.resetProcessedSql();
		
		// test multiple hints
		s.getTransaction().begin();
		query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
				.addQueryHint( "ALL_ROWS" )
				.addQueryHint( "USE_CONCAT" )
				.setParameter( "departmentName", "Sales" );
		results = query.list();
		s.getTransaction().commit();
		s.clear();
		
		assertEquals(results.size(), 2);
		assertTrue(QueryHintTestDialect.getProcessedSql().contains( "select /*+ ALL_ROWS, USE_CONCAT */"));
		
		QueryHintTestDialect.resetProcessedSql();
		
		// ensure the insertion logic can handle a comment appended to the front
		s.getTransaction().begin();
		query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
				.setComment( "this is a test" )
				.addQueryHint( "ALL_ROWS" )
				.setParameter( "departmentName", "Sales" );
		results = query.list();
		s.getTransaction().commit();
		s.clear();
		
		assertEquals(results.size(), 2);
		assertTrue(QueryHintTestDialect.getProcessedSql().contains( "select /*+ ALL_ROWS */"));
		
		QueryHintTestDialect.resetProcessedSql();
		
		// test Criteria
		s.getTransaction().begin();
		Criteria criteria = s.createCriteria( Employee.class )
				.addQueryHint( "ALL_ROWS" )
				.createCriteria( "department" ).add( Restrictions.eq( "name", "Sales" ) );
		results = criteria.list();
		s.getTransaction().commit();
		s.close();
		
		assertEquals(results.size(), 2);
		assertTrue(QueryHintTestDialect.getProcessedSql().contains( "select /*+ ALL_ROWS */"));
	}
	
	/**
	 * Since the query hint is added to the SQL during Loader's executeQueryStatement -> preprocessSQL, rather than
	 * early on during the QueryTranslator or QueryLoader initialization, there's not an easy way to check the full SQL
	 * after completely processing it.  Instead, use this ridiculous hack to ensure Loader actually calls Dialect.
	 * 
	 * TODO: This is terrible.  Better ideas?
	 */
	public static class QueryHintTestDialect extends Oracle8iDialect {
		private static String processedSql;
		
		@Override
		public String getQueryHintString(String sql, List<String> hints) {
			processedSql = super.getQueryHintString( sql, hints );
			return processedSql;
		}
		
		public static String getProcessedSql() {
			return processedSql;
		}
		
		public static void resetProcessedSql() {
			processedSql = "";
		}
	}
	
	@Entity
	public static class Employee {
		@Id
		@GeneratedValue
		public long id;
		
		@ManyToOne
		public Department department;
	}
	
	@Entity
	public static class Department {
		@Id
		@GeneratedValue
		public long id;
		
		public String name;
	}
}
