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
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.Expectation;

/**
 * Normal implementation of BatchKey
 *
 * @author Steve Ebersole
 */
public class BasicBatchKey implements BatchKey {
	private final String comparison;
	private final int statementCount;
	private final Expectation expectation;

	/**
	 * Constructs a BasicBatchKey
	 *
	 * @param comparison A string used to compare batch keys.
	 * @param expectation The expectation for the batch
	 */
	public BasicBatchKey(String comparison, Expectation expectation) {
		this.comparison = comparison;
		this.statementCount = 1;
		this.expectation = expectation;
	}

	@Override
	public Expectation getExpectation() {
		return expectation;
	}

	@Override
	public int getBatchedStatementCount() {
		return statementCount;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final BasicBatchKey that = (BasicBatchKey) o;
		return comparison.equals( that.comparison );
	}

	@Override
	public int hashCode() {
		return comparison.hashCode();
	}

}
