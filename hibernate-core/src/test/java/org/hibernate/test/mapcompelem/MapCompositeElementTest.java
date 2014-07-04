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
package org.hibernate.test.mapcompelem;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class MapCompositeElementTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "mapcompelem/ProductPart.hbm.xml" };
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testMapCompositeElementWithFormula() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Part top = new Part("top", "The top part");
		Part bottom = new Part("bottom", "The bottom part");
		Product prod = new Product("Some Thing");
		prod.getParts().put("Top", top);
		prod.getParts().put("Bottom", bottom);
		s.persist(prod);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		prod = (Product) s.get(Product.class, "Some Thing");
		assertEquals( prod.getParts().size(), 2 );
		prod.getParts().remove("Bottom");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		prod = (Product) s.get(Product.class, "Some Thing");
		assertEquals( prod.getParts().size(), 1 );
		prod.getParts().put("Top", new Part("top", "The brand new top part"));
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		prod = (Product) s.get(Product.class, "Some Thing");
		assertEquals( prod.getParts().size(), 1 );
		assertEquals(  ( (Part) prod.getParts().get("Top") ).getDescription(), "The brand new top part");
		s.delete(prod);
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "<import>")
	public void testQueryMapCompositeElement() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Part top = new Part("top", "The top part");
		Part bottom = new Part("bottom", "The bottom part");
		Product prod = new Product("Some Thing");
		prod.getParts().put("Top", top);
		prod.getParts().put("Bottom", bottom);
		s.persist(prod);
		
		Item item = new Item("123456", prod);
		s.persist(item);

		List list = s.createQuery("select new Part( part.name, part.description ) from Product prod join prod.parts part order by part.name desc").list();
		assertEquals( list.size(), 2 );
		assertTrue( list.get(0) instanceof Part );
		assertTrue( list.get(1) instanceof Part );
		Part part = (Part) list.get(0);
		assertEquals( part.getName(), "top" );
		assertEquals( part.getDescription(), "The top part" );
		
		list = s.createQuery("select new Part( part.name, part.description ) from Product prod join prod.parts part where index(part) = 'Top'").list();
		assertEquals( list.size(), 1 );
		assertTrue( list.get(0) instanceof Part );
		part = (Part) list.get(0);
		assertEquals( part.getName(), "top" );
		assertEquals( part.getDescription(), "The top part" );
		
		list = s.createQuery("from Product p where 'Top' in indices(p.parts)").list();
		assertEquals( list.size(), 1 );
		assertSame( list.get(0), prod );
		
		list = s.createQuery("select i from Item i join i.product p where 'Top' in indices(p.parts)").list();
		assertEquals( list.size(), 1 );
		assertSame( list.get(0), item );
		
		list = s.createQuery("from Item i where 'Top' in indices(i.product.parts)").list();
		assertEquals( list.size(), 1 );
		assertSame( list.get(0), item );
		
		s.delete(item);
		s.delete(prod);
		t.commit();
		s.close();
	}

}

