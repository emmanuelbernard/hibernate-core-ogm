/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hibernate.engine.jdbc.BinaryStream;

/**
 * Implementation of {@link BinaryStream}
 *
 * @author Steve Ebersole
 */
public class BinaryStreamImpl extends ByteArrayInputStream implements BinaryStream {
	private final int length;

	/**
	 * Constructs a BinaryStreamImpl
	 *
	 * @param bytes The bytes to use backing the stream
	 */
	public BinaryStreamImpl(byte[] bytes) {
		super( bytes );
		this.length = bytes.length;
	}

	public InputStream getInputStream() {
		return this;
	}

	public byte[] getBytes() {
		// from ByteArrayInputStream
		return buf;
	}

	public long getLength() {
		return length;
	}

	@Override
	public void release() {
		try {
			super.close();
		}
		catch (IOException ignore) {
		}
	}
}
