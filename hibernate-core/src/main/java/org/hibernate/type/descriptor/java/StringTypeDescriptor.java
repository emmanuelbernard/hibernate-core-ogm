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
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link String} handling.
 *
 * @author Steve Ebersole
 */
public class StringTypeDescriptor extends AbstractTypeDescriptor<String> {
	public static final StringTypeDescriptor INSTANCE = new StringTypeDescriptor();

	public StringTypeDescriptor() {
		super( String.class );
	}

	public String toString(String value) {
		return value;
	}

	public String fromString(String string) {
		return string;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( value );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( value );
		}
		if ( DataHelper.isNClob( type ) ) {
			return (X) options.getLobCreator().createNClob( value );
		}

		throw unknownUnwrap( type );
	}

	public <X> String wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return (String) value;
		}
		if ( Reader.class.isInstance( value ) ) {
			return DataHelper.extractString( (Reader) value );
		}
		if ( Clob.class.isInstance( value ) ) {
			return DataHelper.extractString( (Clob) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
