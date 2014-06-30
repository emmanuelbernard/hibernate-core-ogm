/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;


/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-4992" )
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
@FailureExpectedWithNewMetamodel(message = "The mapping seems really off.  Ex: Many of the classes have an inverse "
		+ "<one-to-many class=\"Info\"/> w/ a <key column=\"col_plan\"/>, but the Info class has no associations.")
public class VersionedEntityWithInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {
	public String[] getMappings() {
//		return new String[] { "immutable/entitywithmutablecollection/inverse/ContractVariationVersionedOneToManyJoin.hbm.xml" };
		// TODO: force it to blow up -- some of the abstract methods pass, so the builds will fail w/o this
		return null;
	}

	protected boolean checkUpdateCountsAfterAddingExistingElement() {
		return false;
	}

	protected boolean checkUpdateCountsAfterRemovingElementWithoutDelete() {
		return false;
	}
}
