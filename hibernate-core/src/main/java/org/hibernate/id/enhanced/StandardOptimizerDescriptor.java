/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.id.enhanced;

import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * Enumeration of the standard Hibernate id generation optimizers.
 *
 * @author Steve Ebersole
 */
public enum StandardOptimizerDescriptor {
	/**
	 * Describes the optimizer for no optimization
	 */
	NONE( "none", NoopOptimizer.class ),
	/**
	 * Describes the optimizer for using a custom "hilo" algorithm optimization
	 */
	HILO( "hilo", HiLoOptimizer.class ),
	/**
	 * Describes the optimizer for using a custom "hilo" algorithm optimization, following the legacy
	 * Hibernate hilo algorithm
	 */
	LEGACY_HILO( "legacy-hilo", LegacyHiLoAlgorithmOptimizer.class ),
	/**
	 * Describes the optimizer for use with tables/sequences that store the chunk information.  Here, specifically the
	 * hi value is stored in the database.
	 */
	POOLED( "pooled", PooledOptimizer.class, true ),
	/**
	 * Describes the optimizer for use with tables/sequences that store the chunk information.  Here, specifically the
	 * lo value is stored in the database.
	 */
	POOLED_LO( "pooled-lo", PooledLoOptimizer.class, true );

	private static final Logger log = Logger.getLogger( StandardOptimizerDescriptor.class );

	private final String externalName;
	private final Class<? extends Optimizer> optimizerClass;
	private final boolean isPooled;

	private StandardOptimizerDescriptor(String externalName, Class<? extends Optimizer> optimizerClass) {
		this( externalName, optimizerClass, false );
	}

	private StandardOptimizerDescriptor(String externalName, Class<? extends Optimizer> optimizerClass, boolean pooled) {
		this.externalName = externalName;
		this.optimizerClass = optimizerClass;
		this.isPooled = pooled;
	}

	public String getExternalName() {
		return externalName;
	}

	public Class<? extends Optimizer> getOptimizerClass() {
		return optimizerClass;
	}

	public boolean isPooled() {
		return isPooled;
	}

	/**
	 * Interpret the incoming external name into the appropriate enum value
	 *
	 * @param externalName The external name
	 *
	 * @return The corresponding enum value; if no external name is supplied, {@link #NONE} is returned; if an
	 * unrecognized external name is supplied, {@code null} is returned
	 */
	public static StandardOptimizerDescriptor fromExternalName(String externalName) {
		if ( StringHelper.isEmpty( externalName ) ) {
			log.debug( "No optimizer specified, using NONE as default" );
			return NONE;
		}
		else if ( NONE.externalName.equals( externalName ) ) {
			return NONE;
		}
		else if ( HILO.externalName.equals( externalName ) ) {
			return HILO;
		}
		else if ( LEGACY_HILO.externalName.equals( externalName ) ) {
			return LEGACY_HILO;
		}
		else if ( POOLED.externalName.equals( externalName ) ) {
			return POOLED;
		}
		else if ( POOLED_LO.externalName.equals( externalName ) ) {
			return POOLED_LO;
		}
		else {
			log.debugf( "Unknown optimizer key [%s]; returning null assuming Optimizer impl class name", externalName );
			return null;
		}
	}
}
