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
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public class ConnectionObserverStatsBridge implements ConnectionObserver, Serializable {
	private final SessionFactoryImplementor sessionFactory;

	public ConnectionObserverStatsBridge(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void physicalConnectionObtained(Connection connection) {
		if ( sessionFactory.getStatistics().isStatisticsEnabled() ) {
			sessionFactory.getStatisticsImplementor().connect();
		}
	}

	@Override
	public void physicalConnectionReleased() {
	}

	@Override
	public void logicalConnectionClosed() {
	}

	@Override
	public void statementPrepared() {
		if ( sessionFactory.getStatistics().isStatisticsEnabled() ) {
			sessionFactory.getStatisticsImplementor().prepareStatement();
		}
	}
}
