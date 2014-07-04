/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.convert;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Test;

import org.joda.time.LocalDate;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8842" )
public class BasicJodaTimeConversionTest extends BaseUnitTestCase {
	static int callsToConverter = 0;

	public static class JodaLocalDateConverter implements AttributeConverter<LocalDate, Date> {
		public Date convertToDatabaseColumn(LocalDate localDate) {
			callsToConverter++;
			return localDate.toDate();
		}

		public LocalDate convertToEntityAttribute(Date date) {
			callsToConverter++;
			return LocalDate.fromDateFields( date );
		}
	}

	@Entity( name = "TheEntity" )
	public static class TheEntity {
		@Id
		public Integer id;
		@Convert( converter = JodaLocalDateConverter.class )
		public LocalDate theDate;

		public TheEntity() {
		}

		public TheEntity(Integer id, LocalDate theDate) {
			this.id = id;
			this.theDate = theDate;
		}
	}

	@Test
	@FailureExpectedWithNewMetamodel( jiraKey = "HHH-8932" )
	public void testSimpleConvertUsage() throws MalformedURLException {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( TheEntity.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		final EntityPersister ep = emf.unwrap( SessionFactoryImplementor.class ).getEntityPersister( TheEntity.class.getName() );
		final Type theDatePropertyType = ep.getPropertyType( "theDate" );
		final AttributeConverterTypeAdapter type = assertTyping( AttributeConverterTypeAdapter.class, theDatePropertyType );
		assertTyping( JodaLocalDateConverter.class, type.getAttributeConverter() );

		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.persist( new TheEntity( 1, new LocalDate() ) );
			em.getTransaction().commit();
			em.close();

			assertEquals( 1, callsToConverter );

			em = emf.createEntityManager();
			em.getTransaction().begin();
			em.find( TheEntity.class, 1 );
			em.getTransaction().commit();
			em.close();

			assertEquals( 2, callsToConverter );

			em = emf.createEntityManager();
			em.getTransaction().begin();
			em.createQuery( "delete TheEntity" ).executeUpdate();
			em.getTransaction().commit();
			em.close();
		}
		finally {
			emf.close();
		}
	}
}
