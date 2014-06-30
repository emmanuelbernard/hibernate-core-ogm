/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.spi;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;

import org.hibernate.HibernateException;
import org.hibernate.TypeMismatchException;
import org.hibernate.hql.internal.QueryExecutionRequestException;
import org.hibernate.jpa.QueryHints;

/**
 * Base class for implementing both {@link javax.persistence.Query} and {@link javax.persistence.TypedQuery}, including
 * query references built from criteria queries.
 * <p/>
 * Not intended as base for {@link javax.persistence.StoredProcedureQuery}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractQueryImpl<X> extends BaseQueryImpl implements TypedQuery<X> {
	public AbstractQueryImpl(HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
	}

	protected HibernateEntityManagerImplementor getEntityManager() {
		return entityManager();
	}

	/**
	 * Actually execute the update; all pre-requisites have been checked.
	 *
	 * @return The number of "affected rows".
	 */
	protected abstract int internalExecuteUpdate();

	@Override
	@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
	public int executeUpdate() {
		checkOpen( true );
		try {
			if ( ! entityManager().isTransactionInProgress() ) {
				entityManager().throwPersistenceException(
						new TransactionRequiredException(
								"Executing an update/delete query"
						)
				);
				return 0;
			}
			return internalExecuteUpdate();
		}
		catch ( QueryExecutionRequestException he) {
			throw new IllegalStateException(he);
		}
		catch( TypeMismatchException e ) {
			throw new IllegalArgumentException(e);
		}
		catch ( HibernateException he) {
			entityManager().throwPersistenceException( he );
			return 0;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setMaxResults(int maxResults) {
		return (AbstractQueryImpl<X>) super.setMaxResults( maxResults );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setFirstResult(int firstResult) {
		return (AbstractQueryImpl<X>) super.setFirstResult( firstResult );
	}

	@Override
	@SuppressWarnings( {"deprecation"})
	public AbstractQueryImpl<X> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public Set<String> getSupportedHints() {
		return QueryHints.getDefinedHints();
	}

	private javax.persistence.LockModeType jpaLockMode = javax.persistence.LockModeType.NONE;

	@Override
	@SuppressWarnings({ "unchecked" })
	public TypedQuery<X> setLockMode(javax.persistence.LockModeType lockModeType) {
		checkOpen( true );

		if ( isNativeSqlQuery() ) {
			throw new IllegalStateException( "Illegal attempt to set lock mode on a native SQL query" );
		}

		if ( ! LockModeType.NONE.equals(lockModeType)) {
			if ( ! isSelectQuery() ) {
				throw new IllegalStateException( "Illegal attempt to set lock mode on a non-SELECT query" );
			}
		}
		if ( ! canApplyAliasSpecificLockModeHints() ) {
			throw new IllegalStateException( "Not a JPAQL/Criteria query" );
		}

		this.jpaLockMode = lockModeType;
		internalApplyLockMode( lockModeType );
		return this;
	}

	@Override
	public javax.persistence.LockModeType getLockMode() {
		checkOpen( false );

		if ( isNativeSqlQuery() ) {
			throw new IllegalStateException( "Illegal attempt to set lock mode on a native SQL query" );
		}

		if ( ! isSelectQuery() ) {
			throw new IllegalStateException( "Illegal attempt to set lock mode on a non-SELECT query" );
		}

		return jpaLockMode;
	}


	// convariant return handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings("unchecked")
	public <T> AbstractQueryImpl<X> setParameter(Parameter<T> param, T value) {
		return (AbstractQueryImpl<X>) super.setParameter( param, value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		return (AbstractQueryImpl<X>) super.setParameter( param, value, temporalType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		return (AbstractQueryImpl<X>) super.setParameter( param, value, temporalType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(String name, Object value) {
		return (AbstractQueryImpl<X>) super.setParameter( name, value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(String name, Calendar value, TemporalType temporalType) {
		return (AbstractQueryImpl<X>) super.setParameter( name, value, temporalType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(String name, Date value, TemporalType temporalType) {
		return (AbstractQueryImpl<X>) super.setParameter( name, value, temporalType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(int position, Object value) {
		return (AbstractQueryImpl<X>) super.setParameter( position, value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(int position, Calendar value, TemporalType temporalType) {
		return (AbstractQueryImpl<X>) super.setParameter( position, value, temporalType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setParameter(int position, Date value, TemporalType temporalType) {
		return (AbstractQueryImpl<X>) super.setParameter( position, value, temporalType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractQueryImpl<X> setFlushMode(FlushModeType jpaFlushMode) {
		return (AbstractQueryImpl<X>) super.setFlushMode( jpaFlushMode );
	}
	
	protected void checkTransaction() {
		if ( jpaLockMode != null && jpaLockMode != LockModeType.NONE ) {
			if ( !getEntityManager().isTransactionInProgress() ) {
				throw new TransactionRequiredException( "no transaction is in progress" );
			}
		}
	}
}
