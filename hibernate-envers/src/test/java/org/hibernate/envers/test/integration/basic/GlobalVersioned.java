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
package org.hibernate.envers.test.integration.basic;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class GlobalVersioned extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity4.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity4 bte1 = new BasicTestEntity4( "x", "y" );
		em.persist( bte1 );
		id1 = bte1.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		bte1 = em.find( BasicTestEntity4.class, id1 );
		bte1.setStr1( "a" );
		bte1.setStr2( "b" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BasicTestEntity4.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		BasicTestEntity4 ver1 = new BasicTestEntity4( id1, "x", "y" );
		BasicTestEntity4 ver2 = new BasicTestEntity4( id1, "a", "b" );

		assert getAuditReader().find( BasicTestEntity4.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( BasicTestEntity4.class, id1, 2 ).equals( ver2 );
	}
}