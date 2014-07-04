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
package org.hibernate.metamodel.archive.scan.internal;

import org.hibernate.metamodel.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.metamodel.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptorFactory;

/**
 * Standard implementation of the Scanner contract, supporting typical archive walking support where
 * the urls we are processing can be treated using normal file handling.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class StandardScanner extends AbstractScannerImpl {
	public StandardScanner() {
		this( StandardArchiveDescriptorFactory.INSTANCE );
	}

	public StandardScanner(ArchiveDescriptorFactory value) {
		super( value );
	}
}
