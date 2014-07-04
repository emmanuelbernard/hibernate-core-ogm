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
package org.hibernate.test.jpa.ql;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class TreatKeywordTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				JoinedEntity.class, JoinedEntitySubclass.class, JoinedEntitySubSubclass.class,
				JoinedEntitySubclass2.class, JoinedEntitySubSubclass2.class,
				DiscriminatorEntity.class, DiscriminatorEntitySubclass.class, DiscriminatorEntitySubSubclass.class
		};
	}

	@Test
	public void testBasicUsageInJoin() {
		// todo : assert invalid naming of non-subclasses in TREAT statement
		Session s = openSession();

		s.createQuery( "from DiscriminatorEntity e join treat(e.other as DiscriminatorEntitySubclass) o" ).list();
		s.createQuery( "from DiscriminatorEntity e join treat(e.other as DiscriminatorEntitySubSubclass) o" ).list();
		s.createQuery( "from DiscriminatorEntitySubclass e join treat(e.other as DiscriminatorEntitySubSubclass) o" ).list();

		s.createQuery( "from JoinedEntity e join treat(e.other as JoinedEntitySubclass) o" ).list();
		s.createQuery( "from JoinedEntity e join treat(e.other as JoinedEntitySubSubclass) o" ).list();
		s.createQuery( "from JoinedEntitySubclass e join treat(e.other as JoinedEntitySubSubclass) o" ).list();

		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8637" )
	public void testFilteringDiscriminatorSubclasses() {
		Session s = openSession();
		s.beginTransaction();
		DiscriminatorEntity root = new DiscriminatorEntity( 1, "root" );
		s.save( root );
		DiscriminatorEntitySubclass child = new DiscriminatorEntitySubclass( 2, "child", root );
		s.save( child );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		// in select clause
		List result = s.createQuery( "select e from DiscriminatorEntity e" ).list();
		assertEquals( 2, result.size() );
		result = s.createQuery( "select treat (e as DiscriminatorEntitySubclass) from DiscriminatorEntity e" ).list();
		assertEquals( 1, result.size() );
		result = s.createQuery( "select treat (e as DiscriminatorEntitySubSubclass) from DiscriminatorEntity e" ).list();
		assertEquals( 0, result.size() );

		// in join
		result = s.createQuery( "from DiscriminatorEntity e inner join e.other" ).list();
		assertEquals( 1, result.size() );
		result = s.createQuery( "from DiscriminatorEntity e inner join treat (e.other as DiscriminatorEntitySubclass)" ).list();
		assertEquals( 0, result.size() );
		result = s.createQuery( "from DiscriminatorEntity e inner join treat (e.other as DiscriminatorEntitySubSubclass)" ).list();
		assertEquals( 0, result.size() );

		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( root );
		s.delete( child );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8637" )
	public void testFilteringJoinedSubclasses() {
		Session s = openSession();
		s.beginTransaction();
		JoinedEntity root = new JoinedEntity( 1, "root" );
		s.save( root );
		JoinedEntitySubclass child = new JoinedEntitySubclass( 2, "child", root );
		s.save( child );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		// in the select clause which causes an implicit inclusion of subclass joins, the test here makes sure that
		// the TREAT-AS effects the join-type used.
		List result = s.createQuery( "select e from JoinedEntity e" ).list();
		assertEquals( 2, result.size() );
		result = s.createQuery( "select treat (e as JoinedEntitySubclass) from JoinedEntity e" ).list();
		assertEquals( 1, result.size() );
		result = s.createQuery( "select treat (e as JoinedEntitySubSubclass) from JoinedEntity e" ).list();
		assertEquals( 0, result.size() );

		// in join
		result = s.createQuery( "from JoinedEntity e inner join e.other" ).list();
		assertEquals( 1, result.size() );
		result = s.createQuery( "from JoinedEntity e inner join treat (e.other as JoinedEntitySubclass)" ).list();
		assertEquals( 0, result.size() );
		result = s.createQuery( "from JoinedEntity e inner join treat (e.other as JoinedEntitySubSubclass)" ).list();
		assertEquals( 0, result.size() );

		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( child );
		s.delete( root );
		s.getTransaction().commit();
		s.close();
	}

	@Entity( name = "JoinedEntity" )
	@Table( name = "JoinedEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedEntity {
		@Id
		public Integer id;
		public String name;
		@ManyToOne( fetch = FetchType.LAZY )
		public JoinedEntity other;

		public JoinedEntity() {
		}

		public JoinedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public JoinedEntity(Integer id, String name, JoinedEntity other) {
			this.id = id;
			this.name = name;
			this.other = other;
		}
	}

	@Entity( name = "JoinedEntitySubclass" )
	@Table( name = "JoinedEntitySubclass" )
	public static class JoinedEntitySubclass extends JoinedEntity {
		public JoinedEntitySubclass() {
		}

		public JoinedEntitySubclass(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubclass(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "JoinedEntitySubSubclass" )
	@Table( name = "JoinedEntitySubSubclass" )
	public static class JoinedEntitySubSubclass extends JoinedEntitySubclass {
		public JoinedEntitySubSubclass() {
		}

		public JoinedEntitySubSubclass(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubSubclass(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "JoinedEntitySubclass2" )
	@Table( name = "JoinedEntitySubclass2" )
	public static class JoinedEntitySubclass2 extends JoinedEntity {
		public JoinedEntitySubclass2() {
		}

		public JoinedEntitySubclass2(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubclass2(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "JoinedEntitySubSubclass2" )
	@Table( name = "JoinedEntitySubSubclass2" )
	public static class JoinedEntitySubSubclass2 extends JoinedEntitySubclass2 {
		public JoinedEntitySubSubclass2() {
		}

		public JoinedEntitySubSubclass2(Integer id, String name) {
			super( id, name );
		}

		public JoinedEntitySubSubclass2(Integer id, String name, JoinedEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "DiscriminatorEntity" )
	@Table( name = "DiscriminatorEntity" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "e_type", discriminatorType = DiscriminatorType.STRING )
	@DiscriminatorValue( "B" )
	public static class DiscriminatorEntity {
		@Id
		public Integer id;
		public String name;
		@ManyToOne( fetch = FetchType.LAZY )
		public DiscriminatorEntity other;

		public DiscriminatorEntity() {
		}

		public DiscriminatorEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public DiscriminatorEntity(
				Integer id,
				String name,
				DiscriminatorEntity other) {
			this.id = id;
			this.name = name;
			this.other = other;
		}
	}

	@Entity( name = "DiscriminatorEntitySubclass" )
	@DiscriminatorValue( "S" )
	public static class DiscriminatorEntitySubclass extends DiscriminatorEntity {
		public DiscriminatorEntitySubclass() {
		}

		public DiscriminatorEntitySubclass(Integer id, String name) {
			super( id, name );
		}

		public DiscriminatorEntitySubclass(
				Integer id,
				String name,
				DiscriminatorEntity other) {
			super( id, name, other );
		}
	}

	@Entity( name = "DiscriminatorEntitySubSubclass" )
	@DiscriminatorValue( "SS" )
	public static class DiscriminatorEntitySubSubclass extends DiscriminatorEntitySubclass {
		public DiscriminatorEntitySubSubclass() {
		}

		public DiscriminatorEntitySubSubclass(Integer id, String name) {
			super( id, name );
		}

		public DiscriminatorEntitySubSubclass(
				Integer id,
				String name,
				DiscriminatorEntity other) {
			super( id, name, other );
		}
	}
}
