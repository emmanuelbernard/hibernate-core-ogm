package org.hibernate.envers.test.integration.customtype;

import javax.persistence.EntityManager;
import java.util.List;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.customtype.EnumTypeEntity;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7780")
public class EnumTypeTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {EnumTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		EnumTypeEntity entity = new EnumTypeEntity( EnumTypeEntity.E1.X, EnumTypeEntity.E2.A );
		em.persist( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testEnumRepresentation() {
		EntityManager entityManager = getEntityManager();
		List<Object[]> values = entityManager.createNativeQuery(
				"SELECT enum1, enum2 FROM EnumTypeEntity_AUD ORDER BY rev ASC"
		).getResultList();
		entityManager.close();

		Assert.assertNotNull( values );
		Assert.assertEquals( 1, values.size() );
		Object[] results = values.get( 0 );
		Assert.assertEquals( 2, results.length );
		Assert.assertEquals( "X", results[0] );
		// Compare the Strings to account for, as an example, Oracle
		// returning a BigDecimal instead of an int.
		Assert.assertEquals( "0", results[1] + "" );
	}
}
