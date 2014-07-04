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
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect compatible with SAP DB.
 *
 * @author Brad Clow
 */
public class SAPDBDialect extends Dialect {
	/**
	 * Constructs a SAPDBDialect
	 */
	public SAPDBDialect() {
		super();
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.BIGINT, "fixed(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "fixed(3,0)" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "long byte" );
		registerColumnType( Types.NUMERIC, "fixed($p,$s)" );
		registerColumnType( Types.CLOB, "long varchar" );
		registerColumnType( Types.BLOB, "long byte" );

		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );

		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power" ) );
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSQLFunction( "cosh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSQLFunction( "sinh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSQLFunction( "tanh", StandardBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardBasicTypes.DOUBLE ) );

		registerFunction( "round", new StandardSQLFunction( "round" ) );
		registerFunction( "trunc", new StandardSQLFunction( "trunc" ) );
		registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
		registerFunction( "floor", new StandardSQLFunction( "floor" ) );
		registerFunction( "greatest", new StandardSQLFunction( "greatest" ) );
		registerFunction( "least", new StandardSQLFunction( "least" ) );

		registerFunction( "time", new StandardSQLFunction( "time", StandardBasicTypes.TIME ) );
		registerFunction( "timestamp", new StandardSQLFunction( "timestamp", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "date", new StandardSQLFunction( "date", StandardBasicTypes.DATE ) );
		registerFunction( "microsecond", new StandardSQLFunction( "microsecond", StandardBasicTypes.INTEGER ) );

		registerFunction( "second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "second(?1)" ) );
		registerFunction( "minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "minute(?1)" ) );
		registerFunction( "hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "hour(?1)" ) );
		registerFunction( "day", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "day(?1)" ) );
		registerFunction( "month", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "month(?1)" ) );
		registerFunction( "year", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "year(?1)" ) );

		registerFunction( "extract", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1(?3)" ) );

		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "weekofyear", new StandardSQLFunction( "weekofyear", StandardBasicTypes.INTEGER ) );

		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSQLFunction( "translate", StandardBasicTypes.STRING ) );
		registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
		registerFunction( "initcap", new StandardSQLFunction( "initcap", StandardBasicTypes.STRING ) );
		registerFunction( "lower", new StandardSQLFunction( "lower", StandardBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "lfill", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
		registerFunction( "rfill", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "upper", new StandardSQLFunction( "upper", StandardBasicTypes.STRING ) );
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.STRING ) );
		registerFunction( "index", new StandardSQLFunction( "index", StandardBasicTypes.INTEGER ) );

		registerFunction( "value", new StandardSQLFunction( "value" ) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
		registerFunction( "locate", new StandardSQLFunction( "index", StandardBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSQLFunction( "value" ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder res = new StringBuilder( 30 )
				.append( " foreign key " )
				.append( constraintName )
				.append( " (" )
				.append( StringHelper.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			res.append( " (" )
					.append( StringHelper.join( ", ", primaryKey ) )
					.append( ')' );
		}

		return res.toString();
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " primary key ";
	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dual";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return "select sequence_name from domain.sequences";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "ignore rollback";
	}

	@Override
	public String generateTemporaryTableName(String baseTableName) {
		return "temp." + super.generateTemporaryTableName( baseTableName );
	}

}
