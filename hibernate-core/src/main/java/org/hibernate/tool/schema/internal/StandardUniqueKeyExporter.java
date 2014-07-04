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
package org.hibernate.tool.schema.internal;

import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Constraint;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Unique constraint Exporter.  Note that it's parameterized for Constraint, rather than UniqueKey.  This is
 * to allow Dialects to decide whether or not to create unique constraints for unique indexes.
 * 
 * @author Brett Meyer
 */
public class StandardUniqueKeyExporter implements Exporter<Constraint> {
	private final Dialect dialect;

	public StandardUniqueKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Constraint constraint, JdbcEnvironment jdbcEnvironment) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}
		
		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) constraint.getTable() ).getTableName()
		);
		StringBuilder sb = new StringBuilder()
				.append( "alter table " )
				.append( tableName )
				.append( " add constraint " )
				.append( constraint.getName().getText( dialect ) )
				.append( " unique ( " );

		final Iterator columnIterator = constraint.getColumns().iterator();
		while ( columnIterator.hasNext() ) {
			Column column = (Column) columnIterator.next();
			sb.append( column.getColumnName().getText( dialect ) );
			if ( columnIterator.hasNext() ) {
				sb.append( ", " );
			}
		}
		sb.append( ")" );
		return new String[] { sb.toString() };
	}

	@Override
	public String[] getSqlDropStrings(Constraint constraint, JdbcEnvironment jdbcEnvironment) {
		if ( ! dialect.dropConstraints() ) {
			return NO_COMMANDS;
		}

		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) constraint.getTable() ).getTableName()
		);
		final StringBuilder sb = new StringBuilder( "alter table " );
		sb.append( tableName );
		sb.append(" drop constraint " );
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			sb.append( "if exists " );
		}
		sb.append( constraint.getName().getText( dialect ) );
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			sb.append( " if exists" );
		}
		return new String[] { sb.toString() };
	}
}
