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
package org.hibernate.jpa.test.emops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;

/**
 * @author Emmanuel Bernard
 */
public class FlushTest extends BaseEntityManagerFunctionalTestCase {
	private static Set<String> names = namesSet();
	private static Set<String> namesSet() {
		HashSet<String> names = new HashSet<String>();
		names.add("Toonses");
		names.add("Sox");
		names.add("Winnie");
		names.add("Junior");
		return names;
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Pet.class,
				Dog.class,
				Cat.class,
				Decorate.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "EJBTHREE-722" )
	public void testFlushOnDetached() throws Exception {
		EntityManager manager = getOrCreateEntityManager( );

		manager.getTransaction().begin();
		Pet p1 = createCat("Toonses", 15.0, 9, manager);
		manager.flush();
		manager.clear();

		Pet p2 = createCat("Sox", 10.0, 5, manager);
		manager.flush();
		manager.clear();

		Pet p3 = createDog("Winnie", 70.0, 5, manager);
		manager.flush();
		manager.clear();

		Pet p4 = createDog("Junior", 11.0, 1, manager);
		manager.flush();
		manager.clear();

		Decorate d1 = createDecorate("Test", p1, manager);
		manager.flush();
		manager.clear();

		Decorate d2 = createDecorate("Test2", p2, manager);
		manager.flush();
		manager.clear();

		List l = findByWeight(14.0, manager);
		manager.flush();
		manager.clear();
		for (Object o : l) {
			Assert.assertTrue( names.contains( ((Pet) o).getName() ) );
		}

		Collection<Decorate> founds = getDecorate(manager);
		manager.flush();
		manager.clear();
		for (Decorate value : founds) {
			Assert.assertTrue( names.contains( value.getPet().getName() ) );
		}
		manager.getTransaction().rollback();
		
		manager.close();
	}

	private Dog createDog(String name, double weight, int bones, EntityManager manager) {
		Dog dog = new Dog();
		dog.setName(name);
		dog.setWeight(weight);
		dog.setNumBones(bones);
		manager.persist(dog);
		return dog;
	}

	private Cat createCat(String name, double weight, int lives, EntityManager manager) {
		Cat cat = new Cat();
		cat.setName(name);
		cat.setWeight(weight);
		cat.setLives(lives);
		manager.persist(cat);
		return cat;
	}

	private List findByWeight(double weight, EntityManager manager) {
		return manager.createQuery(
				"select p from Pet p where p.weight < :weight").setParameter(
				"weight", weight).getResultList();
	}

	private Decorate createDecorate(String name, Pet pet, EntityManager manager) {
		Decorate dec = new Decorate();
		dec.setName(name);
		dec.setPet(pet);
		manager.persist(dec);
		return dec;
	}

	private Collection<Decorate> getDecorate(EntityManager manager) {
		Collection<Decorate> founds = new ArrayList<Decorate>();
		Query query = manager.createQuery("SELECT o FROM Decorate o");
		List list = query.getResultList();
		for (Object obj : list) {
			if (obj instanceof Decorate) {
				Decorate decorate = (Decorate) obj;
				founds.add( decorate );
				decorate.getPet().getName(); //load
			}
		}
		return founds;
	}

}
