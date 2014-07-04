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
package org.hibernate;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.type.Type;

/**
 * A result iterator that allows moving around within the results
 * by arbitrary increments. The <tt>Query</tt> / <tt>ScrollableResults</tt>
 * pattern is very similar to the JDBC <tt>PreparedStatement</tt>/
 * <tt>ResultSet</tt> pattern and the semantics of methods of this interface
 * are similar to the similarly named methods on <tt>ResultSet</tt>.<br>
 * <br>
 * Contrary to JDBC, columns of results are numbered from zero.
 *
 * @see Query#scroll()
 *
 * @author Gavin King
 */
public interface ScrollableResults {
	/**
	 * Advance to the next result.
	 *
	 * @return {@code true} if there is another result
	 */
	public boolean next();

	/**
	 * Retreat to the previous result.
	 *
	 * @return {@code true} if there is a previous result
	 */
	public boolean previous();

	/**
	 * Scroll the specified number of positions from the current position.
	 *
	 * @param positions a positive (forward) or negative (backward) number of rows
	 *
	 * @return {@code true} if there is a result at the new location
	 */
	public boolean scroll(int positions);

	/**
	 * Go to the last result.
	 *
	 * @return {@code true} if there are any results
	 */
	public boolean last();

	/**
	 * Go to the first result.
	 *
	 * @return {@code true} if there are any results
	 */
	public boolean first();

	/**
	 * Go to a location just before first result,  This is the location of the cursor on a newly returned
	 * scrollable result.
	 */
	public void beforeFirst();

	/**
	 * Go to a location just after the last result.
	 */
	public void afterLast();

	/**
	 * Is this the first result?
	 *
	 * @return {@code true} if this is the first row of results, otherwise {@code false}
	 */
	public boolean isFirst();

	/**
	 * Is this the last result?
	 *
	 * @return {@code true} if this is the last row of results.
	 */
	public boolean isLast();

	/**
	 * Get the current position in the results. The first position is number 0 (unlike JDBC).
	 *
	 * @return The current position number, numbered from 0; -1 indicates that there is no current row
	 */
	public int getRowNumber();

	/**
	 * Set the current position in the result set.  Can be numbered from the first position (positive number) or
	 * the last row (negative number).
	 *
	 * @param rowNumber the row number.  A positive number indicates a value numbered from the first row; a
	 * negative number indicates a value numbered from the last row.
	 *
	 * @return true if there is a row at that row number
	 */
	public boolean setRowNumber(int rowNumber);

	/**
	 * Release resources immediately.
	 */
	public void close();

	/**
	 * Get the current row of results.
	 *
	 * @return The array of results
	 */
	public Object[] get();

	/**
	 * Get the <tt>i</tt>th object in the current row of results, without
	 * initializing any other results in the row. This method may be used
	 * safely, regardless of the type of the column (ie. even for scalar
	 * results).
	 *
	 * @param i the column, numbered from zero
	 *
	 * @return The requested result object; may return {@code null}
	 *
	 * @throws IndexOutOfBoundsException If i is an invalid index.
	 */
	public Object get(int i);

	/**
	 * Get the type of the <tt>i</tt>th column of results.
	 *
	 * @param i the column, numbered from zero
	 *
	 * @return the Hibernate type
	 *
	 * @throws IndexOutOfBoundsException If i is an invalid index.
	 */
	public Type getType(int i);

	/**
	 * Convenience method to read an integer.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as an integer
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Integer getInteger(int col);

	/**
	 * Convenience method to read a long.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a long
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Long getLong(int col);

	/**
	 * Convenience method to read a float.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a float
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Float getFloat(int col);

	/**
	 * Convenience method to read a boolean.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a boolean
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Boolean getBoolean(int col);

	/**
	 * Convenience method to read a double.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a double
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Double getDouble(int col);

	/**
	 * Convenience method to read a short.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a short
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Short getShort(int col);

	/**
	 * Convenience method to read a byte.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a byte
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Byte getByte(int col);

	/**
	 * Convenience method to read a char.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a char
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Character getCharacter(int col);

	/**
	 * Convenience method to read a binary (byte[]).
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a binary (byte[])
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public byte[] getBinary(int col);

	/**
	 * Convenience method to read a String using streaming.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a String
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public String getText(int col);

	/**
	 * Convenience method to read a blob.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Blob
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Blob getBlob(int col);

	/**
	 * Convenience method to read a clob.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Clob
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Clob getClob(int col);

	/**
	 * Convenience method to read a string.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a String
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public String getString(int col);

	/**
	 * Convenience method to read a BigDecimal.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a BigDecimal
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public BigDecimal getBigDecimal(int col);

	/**
	 * Convenience method to read a BigInteger.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a BigInteger
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public BigInteger getBigInteger(int col);

	/**
	 * Convenience method to read a Date.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Date
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Date getDate(int col);

	/**
	 * Convenience method to read a Locale.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Locale
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Locale getLocale(int col);

	/**
	 * Convenience method to read a Calendar.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a Calendar
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public Calendar getCalendar(int col);

	/**
	 * Convenience method to read a TimeZone.
	 *
	 * @param col The column, numbered from zero
	 *
	 * @return The column value as a TimeZone
	 *
	 * @throws IndexOutOfBoundsException If col is an invalid index.
	 */
	public TimeZone getTimeZone(int col);
}






