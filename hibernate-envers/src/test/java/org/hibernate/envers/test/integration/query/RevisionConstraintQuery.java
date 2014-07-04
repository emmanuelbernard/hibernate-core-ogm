/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.test.integration.query;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class RevisionConstraintQuery extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrIntTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
		StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );

		em.persist( site1 );
		em.persist( site2 );

		id1 = site1.getId();
		Integer id2 = site2.getId();

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setStr1( "d" );
		site2.setNumber( 20 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setNumber( 1 );
		site2.setStr1( "z" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setNumber( 5 );
		site2.setStr1( "a" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsLtQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().distinct() )
				.add( AuditEntity.revisionNumber().lt( 3 ) )
				.getResultList();

		Assert.assertEquals( Arrays.asList( 1, 2 ), result );
	}

	@Test
	public void testRevisionsGeQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().distinct() )
				.add( AuditEntity.revisionNumber().ge( 2 ) )
				.getResultList();

		Assert.assertEquals( TestTools.makeSet( 2, 3, 4 ), new HashSet( result ) );
	}

	@Test
	public void testRevisionsLeWithPropertyQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.revisionNumber().le( 3 ) )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		Assert.assertEquals( Arrays.asList( 1 ), result );
	}

	@Test
	public void testRevisionsGtWithPropertyQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.revisionNumber().gt( 1 ) )
				.add( AuditEntity.property( "number" ).lt( 10 ) )
				.getResultList();

		Assert.assertEquals( Arrays.asList( 3, 4 ), result );
	}

	@Test
	public void testRevisionProjectionQuery() {
		Object[] result = (Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().max() )
				.addProjection( AuditEntity.revisionNumber().count() )
				.addProjection( AuditEntity.revisionNumber().countDistinct() )
				.addProjection( AuditEntity.revisionNumber().min() )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Assert.assertEquals( Integer.valueOf( 4 ), result[0] );
		Assert.assertEquals( Long.valueOf( 4 ), result[1] );
		Assert.assertEquals( Long.valueOf( 4 ), result[2] );
		Assert.assertEquals( Integer.valueOf( 1 ), result[3] );
	}

	@Test
	public void testRevisionOrderQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.id().eq( id1 ) )
				.addOrder( AuditEntity.revisionNumber().desc() )
				.getResultList();

		Assert.assertEquals( Arrays.asList( 4, 3, 2, 1 ), result );
	}

	@Test
	public void testRevisionCountQuery() {
		// The query shouldn't be ordered as always, otherwise - we get an exception.
		Object result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().count() )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Assert.assertEquals( Long.valueOf( 4 ), result );
	}

	@Test
	public void testRevisionTypeEqQuery() {
		// The query shouldn't be ordered as always, otherwise - we get an exception.
		List results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.id().eq( id1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.MOD ) )
				.getResultList();

		Assert.assertEquals( 3, results.size() );
		Assert.assertEquals( new StrIntTestEntity( "d", 10, id1 ), results.get( 0 ) );
		Assert.assertEquals( new StrIntTestEntity( "d", 1, id1 ), results.get( 1 ) );
		Assert.assertEquals( new StrIntTestEntity( "d", 5, id1 ), results.get( 2 ) );
	}

	@Test
	public void testRevisionTypeNeQuery() {
		// The query shouldn't be ordered as always, otherwise - we get an exception.
		List results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.id().eq( id1 ) )
				.add( AuditEntity.revisionType().ne( RevisionType.MOD ) )
				.getResultList();

		Assert.assertEquals( 1, results.size() );
		Assert.assertEquals( new StrIntTestEntity( "a", 10, id1 ), results.get( 0 ) );
	}
}