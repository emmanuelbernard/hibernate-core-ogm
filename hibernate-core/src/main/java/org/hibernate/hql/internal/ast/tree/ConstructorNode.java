/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.internal.ast.tree;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.hql.internal.ast.DetailedSemanticException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents a constructor (new) in a SELECT.
 *
 * @author josh
 */
public class ConstructorNode extends SelectExpressionList implements AggregatedSelectExpression {
	private Class resultType;
	private Constructor constructor;
	private Type[] constructorArgumentTypes;
	private boolean isMap;
	private boolean isList;

	@Override
	public ResultTransformer getResultTransformer() {
		if ( constructor != null ) {
			return new AliasToBeanConstructorResultTransformer( constructor );
		}
		else if ( isMap ) {
			return Transformers.ALIAS_TO_ENTITY_MAP;
		}
		else if ( isList ) {
			return Transformers.TO_LIST;
		}
		throw new QueryException( "Unable to determine proper dynamic-instantiation tranformer to use." );
	}

	private String[] aggregatedAliases;

	@Override
	public String[] getAggregatedAliases() {
		if ( aggregatedAliases == null ) {
			aggregatedAliases = buildAggregatedAliases();
		}
		return aggregatedAliases;
	}

	private String[] buildAggregatedAliases() {
		SelectExpression[] selectExpressions = collectSelectExpressions();
		String[] aliases = new String[selectExpressions.length];
		for ( int i = 0; i < selectExpressions.length; i++ ) {
			String alias = selectExpressions[i].getAlias();
			aliases[i] = alias == null ? Integer.toString( i ) : alias;
		}
		return aliases;
	}

	@Override
	public void setScalarColumn(int i) throws SemanticException {
		SelectExpression[] selectExpressions = collectSelectExpressions();
		// Invoke setScalarColumnText on each constructor argument.
		for ( int j = 0; j < selectExpressions.length; j++ ) {
			SelectExpression selectExpression = selectExpressions[j];
			selectExpression.setScalarColumn( j );
		}
	}

	@Override
	public int getScalarColumnIndex() {
		return -1;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		SelectExpression[] selectExpressions = collectSelectExpressions();
		// Invoke setScalarColumnText on each constructor argument.
		for ( int j = 0; j < selectExpressions.length; j++ ) {
			SelectExpression selectExpression = selectExpressions[j];
			selectExpression.setScalarColumnText( j );
		}
	}

	@Override
	protected AST getFirstSelectExpression() {
		// Collect the select expressions, skip the first child because it is the class name.
		return getFirstChild().getNextSibling();
	}

	@Override
	public Class getAggregationResultType() {
		return resultType;
	}

	/**
	 * @deprecated (tell clover to ignore this method)
	 */
	@Deprecated
	@Override
	public Type getDataType() {
/*
		// Return the type of the object created by the constructor.
		AST firstChild = getFirstChild();
		String text = firstChild.getText();
		if ( firstChild.getType() == SqlTokenTypes.DOT ) {
			DotNode dot = ( DotNode ) firstChild;
			text = dot.getPath();
		}
		return getSessionFactoryHelper().requireEntityType( text );
*/
		throw new UnsupportedOperationException( "getDataType() is not supported by ConstructorNode!" );
	}

	public void prepare() throws SemanticException {
		constructorArgumentTypes = resolveConstructorArgumentTypes();
		String path = ( (PathNode) getFirstChild() ).getPath();
		if ( "map".equals( path.toLowerCase() ) ) {
			isMap = true;
			resultType = Map.class;
		}
		else if ( "list".equals( path.toLowerCase() ) ) {
			isList = true;
			resultType = List.class;
		}
		else {
			constructor = resolveConstructor( path );
			resultType = constructor.getDeclaringClass();
		}
	}

	private Type[] resolveConstructorArgumentTypes() throws SemanticException {
		SelectExpression[] argumentExpressions = collectSelectExpressions();
		if ( argumentExpressions == null ) {
			// return an empty Type array
			return new Type[] {};
		}

		Type[] types = new Type[argumentExpressions.length];
		for ( int x = 0; x < argumentExpressions.length; x++ ) {
			types[x] = argumentExpressions[x].getDataType();
		}
		return types;
	}

	private Constructor resolveConstructor(String path) throws SemanticException {
		String importedClassName = getSessionFactoryHelper().getImportedClassName( path );
		String className = StringHelper.isEmpty( importedClassName ) ? path : importedClassName;
		if ( className == null ) {
			throw new SemanticException( "Unable to locate class [" + path + "]" );
		}
		try {
			Class holderClass = ReflectHelper.classForName( className );
			return ReflectHelper.getConstructor( holderClass, constructorArgumentTypes );
		}
		catch (ClassNotFoundException e) {
			throw new DetailedSemanticException( "Unable to locate class [" + className + "]", e );
		}
		catch (PropertyNotFoundException e) {
			// this is the exception returned by ReflectHelper.getConstructor() if it cannot
			// locate an appropriate constructor
			throw new DetailedSemanticException( formatMissingContructorExceptionMessage( className ), e );
		}
	}

	// HHH-8068 -- provide a more helpful message
	private String formatMissingContructorExceptionMessage(String className) {
		String[] params = new String[constructorArgumentTypes.length];
		for ( int j = 0; j < constructorArgumentTypes.length; j++ ) {
			params[j] = constructorArgumentTypes[j] instanceof PrimitiveType
					? ( (PrimitiveType) constructorArgumentTypes[j] ).getPrimitiveClass().getName()
					: constructorArgumentTypes[j].getReturnedClass().getName();
		}
		String formattedList = params.length == 0 ? "no arguments constructor" : StringHelper.join( ", ", params );
		return String.format(
				"Unable to locate appropriate constructor on class [%s]. Expected arguments are: %s",
				className, formattedList
		);
	}

	public Constructor getConstructor() {
		return constructor;
	}

	public List getConstructorArgumentTypeList() {
		return Arrays.asList( constructorArgumentTypes );
	}

	@Override
	public List getAggregatedSelectionTypeList() {
		return getConstructorArgumentTypeList();
	}

	@Override
	public FromElement getFromElement() {
		return null;
	}

	@Override
	public boolean isConstructor() {
		return true;
	}

	@Override
	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	@Override
	public boolean isScalar() {
		// Constructors are always considered scalar results.
		return true;
	}

	@Override
	public void setAlias(String alias) {
		throw new UnsupportedOperationException( "constructor may not be aliased" );
	}

	@Override
	public String getAlias() {
		throw new UnsupportedOperationException( "constructor may not be aliased" );
	}
}
