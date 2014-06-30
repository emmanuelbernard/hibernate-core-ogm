/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.dialect.Dialect;

/**
 * Models a table's primary key.
 * <p/>
 * NOTE : This need not be a physical primary key; we just mean a column or columns which uniquely identify rows in
 * the table.  Of course it is recommended to define proper integrity constraints, including primary keys.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PrimaryKey extends AbstractConstraint {

	protected PrimaryKey(TableSpecification table) {
		super( table, null );
	}

	// TODO: Can this be removed?
	public String sqlConstraintStringInCreateTable(Dialect dialect) {
		StringBuilder buf = new StringBuilder("primary key (");
		boolean first = true;
		for ( Column column : getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append(", ");
			}
			buf.append( column.getColumnName().getText( dialect ) );
		}
		return buf.append(')').toString();
	}
	
	@Override
	public String getExportIdentifier() {
		return getTable().getLoggableValueQualifier() + ".PK";
	}

}
