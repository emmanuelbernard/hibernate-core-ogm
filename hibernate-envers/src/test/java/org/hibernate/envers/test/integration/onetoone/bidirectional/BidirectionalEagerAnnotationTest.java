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
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerAnnotationRefEdOneToOne;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerAnnotationRefIngOneToOne;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertNotNull;

/**
 * @author Erik-Berndt Scheper
 */
@TestForIssue(jiraKey = "HHH-3854")
public class BidirectionalEagerAnnotationTest extends BaseEnversJPAFunctionalTestCase {
	private Integer refIngId1 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BidirectionalEagerAnnotationRefEdOneToOne.class,
				BidirectionalEagerAnnotationRefIngOneToOne.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		BidirectionalEagerAnnotationRefEdOneToOne ed1 = new BidirectionalEagerAnnotationRefEdOneToOne();
		BidirectionalEagerAnnotationRefIngOneToOne ing1 = new BidirectionalEagerAnnotationRefIngOneToOne();
		ed1.setData( "referredEntity1" );
		ed1.setRefIng( ing1 );
		ing1.setData( "referringEntity" );
		ing1.setRefedOne( ed1 );
		em.persist( ed1 );
		em.persist( ing1 );
		em.getTransaction().commit();

		refIngId1 = ing1.getId();

		em.close();
	}

	@Test
	public void testNonProxyObjectTraversing() {
		BidirectionalEagerAnnotationRefIngOneToOne referencing =
				getAuditReader().find( BidirectionalEagerAnnotationRefIngOneToOne.class, refIngId1, 1 );
		assertNotNull( referencing.getRefedOne().getData() );
	}
}