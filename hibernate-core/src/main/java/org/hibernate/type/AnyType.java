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
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.proxy.LazyInitializer;

import org.dom4j.Node;

/**
 * Handles "any" mappings
 * 
 * @author Gavin King
 */
public class AnyType extends AbstractType implements CompositeType, AssociationType {
	private final TypeFactory.TypeScope scope;
	private final Type identifierType;
	private final Type discriminatorType;

	/**
	 * Intended for use only from legacy {@link ObjectType} type definition
	 *
	 * @param discriminatorType
	 * @param identifierType
	 */
	protected AnyType(Type discriminatorType, Type identifierType) {
		this( null, discriminatorType, identifierType );
	}

	public AnyType(TypeFactory.TypeScope scope, Type discriminatorType, Type identifierType) {
		this.scope = scope;
		this.discriminatorType = discriminatorType;
		this.identifierType = identifierType;
	}

	public Type getIdentifierType() {
		return identifierType;
	}

	public Type getDiscriminatorType() {
		return discriminatorType;
	}


	// general Type metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getName() {
		return "object";
	}

	@Override
	public Class getReturnedClass() {
		return Object.class;
	}

	@Override
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return ArrayHelper.join( discriminatorType.sqlTypes( mapping ), identifierType.sqlTypes( mapping ) );
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return ArrayHelper.join( discriminatorType.dictatedSizes( mapping ), identifierType.dictatedSizes( mapping ) );
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return ArrayHelper.join( discriminatorType.defaultSizes( mapping ), identifierType.defaultSizes( mapping ) );
	}

	@Override
	public Object[] getPropertyValues(Object component, EntityMode entityMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAnyType() {
		return true;
	}

	@Override
	public boolean isAssociationType() {
		return true;
	}

	@Override
	public boolean isComponentType() {
		return true;
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return value;
	}


	// general Type functionality ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int compare(Object x, Object y) {
		if ( x == null ) {
			// if y is also null, return that they are the same (no option for "UNKNOWN")
			// if y is not null, return that y is "greater" (-1 because the result is from the perspective of
			// 		the first arg: x)
			return y == null ? 0 : -1;
		}
		else if ( y == null ) {
			// x is not null, but y is.  return that x is "greater"
			return 1;
		}

		// At this point we know both are non-null.
		final Object xId = extractIdentifier( x );
		final Object yId = extractIdentifier( y );

		return getIdentifierType().compare( xId, yId );
	}

	private Object extractIdentifier(Object entity) {
		final EntityPersister concretePersister = guessEntityPersister( entity );
		return concretePersister == null
				? null
				: concretePersister.getEntityTuplizer().getIdentifier( entity, null );
	}

	private EntityPersister guessEntityPersister(Object object) {
		if ( scope == null ) {
			return null;
		}

		String entityName = null;

		// this code is largely copied from Session's bestGuessEntityName
		Object entity = object;
		if ( entity instanceof HibernateProxy ) {
			final LazyInitializer initializer = ( (HibernateProxy) entity ).getHibernateLazyInitializer();
			if ( initializer.isUninitialized() ) {
				entityName = initializer.getEntityName();
			}
			entity = initializer.getImplementation();
		}

		if ( entityName == null ) {
			for ( EntityNameResolver resolver : scope.resolveFactory().iterateEntityNameResolvers() ) {
				entityName = resolver.resolveEntityName( entity );
				if ( entityName != null ) {
					break;
				}
			}
		}

		if ( entityName == null ) {
			// the old-time stand-by...
			entityName = object.getClass().getName();
		}

		return scope.resolveFactory().getEntityPersister( entityName );
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		return x == y;
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		if ( current == null ) {
			return old != null;
		}
		else if ( old == null ) {
			return true;
		}

		final ObjectTypeCacheEntry holder = (ObjectTypeCacheEntry) old;
		final boolean[] idCheckable = new boolean[checkable.length-1];
		System.arraycopy( checkable, 1, idCheckable, 0, idCheckable.length );
		return ( checkable[0] && !holder.entityName.equals( session.bestGuessEntityName( current ) ) )
				|| identifierType.isModified( holder.id, getIdentifier( current, session ), idCheckable, session );
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		final boolean[] result = new boolean[ getColumnSpan( mapping ) ];
		if ( value != null ) {
			Arrays.fill( result, true );
		}
		return result;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return isDirty( old, current, session );
	}

	@Override
	public int getColumnSpan(Mapping session) {
		return 2;
	}

	@Override
	public Object nullSafeGet(ResultSet rs,	String[] names,	SessionImplementor session,	Object owner)
			throws HibernateException, SQLException {
		return resolveAny(
				(String) discriminatorType.nullSafeGet( rs, names[0], session, owner ),
				(Serializable) identifierType.nullSafeGet( rs, names[1], session, owner ),
				session
		);
	}

	@Override
	public Object hydrate(ResultSet rs,	String[] names,	SessionImplementor session,	Object owner)
			throws HibernateException, SQLException {
		final String entityName = (String) discriminatorType.nullSafeGet( rs, names[0], session, owner );
		final Serializable id = (Serializable) identifierType.nullSafeGet( rs, names[1], session, owner );
		return new ObjectTypeCacheEntry( entityName, id );
	}

	@Override
	public Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		final ObjectTypeCacheEntry holder = (ObjectTypeCacheEntry) value;
		return resolveAny( holder.entityName, holder.id, session );
	}

	private Object resolveAny(String entityName, Serializable id, SessionImplementor session)
			throws HibernateException {
		return entityName==null || id==null
				? null
				: session.internalLoad( entityName, id, false, false );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value,	int index, SessionImplementor session)
			throws HibernateException, SQLException {
		nullSafeSet( st, value, index, null, session );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value,	int index, boolean[] settable, SessionImplementor session)
			throws HibernateException, SQLException {
		Serializable id;
		String entityName;
		if ( value == null ) {
			id = null;
			entityName = null;
		}
		else {
			entityName = session.bestGuessEntityName( value );
			id = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, value, session );
		}

		// discriminatorType is assumed to be single-column type
		if ( settable == null || settable[0] ) {
			discriminatorType.nullSafeSet( st, entityName, index, session );
		}
		if ( settable == null ) {
			identifierType.nullSafeSet( st, id, index+1, session );
		}
		else {
			final boolean[] idSettable = new boolean[ settable.length-1 ];
			System.arraycopy( settable, 1, idSettable, 0, idSettable.length );
			identifierType.nullSafeSet( st, id, index+1, idSettable, session );
		}
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		//TODO: terrible implementation!
		return value == null
				? "null"
				: factory.getTypeHelper()
				.entity( HibernateProxyHelper.getClassWithoutInitializingProxy( value ) )
				.toLoggableString( value, factory );
	}

	@Override
	public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
		final ObjectTypeCacheEntry e = (ObjectTypeCacheEntry) cached;
		return e == null ? null : session.internalLoad( e.entityName, e.id, false, false );
	}

	@Override
	public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
		if ( value == null ) {
			return null;
		}
		else {
			return new ObjectTypeCacheEntry(
					session.bestGuessEntityName( value ),
					ForeignKeys.getEntityIdentifierIfNotUnsaved(
							session.bestGuessEntityName( value ),
							value,
							session
					)
			);
		}
	}

	@Override
	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		if ( original == null ) {
			return null;
		}
		else {
			final String entityName = session.bestGuessEntityName( original );
			final Serializable id = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, original, session );
			return session.internalLoad( entityName, id, false, false );
		}
	}

	@Override
	public Object nullSafeGet(ResultSet rs,	String name, SessionImplementor session, Object owner) {
		throw new UnsupportedOperationException( "object is a multicolumn type" );
	}

	@Override
	public Object semiResolve(Object value, SessionImplementor session, Object owner) {
		throw new UnsupportedOperationException( "any mappings may not form part of a property-ref" );
	}

	@Override
	public void setToXMLNode(Node xml, Object value, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException("any types cannot be stringified");
	}

	@Override
	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		throw new UnsupportedOperationException();
	}



	// CompositeType implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean isMethodOf(Method method) {
		return false;
	}

	private static final String[] PROPERTY_NAMES = new String[] { "class", "id" };

	@Override
	public String[] getPropertyNames() {
		return PROPERTY_NAMES;
	}

	@Override
	public Object getPropertyValue(Object component, int i, SessionImplementor session) throws HibernateException {
		return i==0
				? session.bestGuessEntityName( component )
				: getIdentifier( component, session );
	}

	@Override
	public Object[] getPropertyValues(Object component, SessionImplementor session) throws HibernateException {
		return new Object[] {
				session.bestGuessEntityName( component ),
				getIdentifier( component, session )
		};
	}

	private Serializable getIdentifier(Object value, SessionImplementor session) throws HibernateException {
		try {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved(
					session.bestGuessEntityName( value ),
					value,
					session
			);
		}
		catch (TransientObjectException toe) {
			return null;
		}
	}

	@Override
	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) {
		throw new UnsupportedOperationException();
	}

	private static final boolean[] NULLABILITY = new boolean[] { false, false };

	@Override
	public boolean[] getPropertyNullability() {
		return NULLABILITY;
	}

	@Override
	public Type[] getSubtypes() {
		return new Type[] {discriminatorType, identifierType };
	}

	@Override
	public CascadeStyle getCascadeStyle(int i) {
		return CascadeStyles.NONE;
	}

	@Override
	public FetchMode getFetchMode(int i) {
		return FetchMode.SELECT;
	}


	// AssociationType implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FROM_PARENT;
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return false;
	}

	@Override
	public String getLHSPropertyName() {
		return null;
	}

	public boolean isReferenceToPrimaryKey() {
		return true;
	}

	@Override
	public String getRHSUniqueKeyPropertyName() {
		return null;
	}

	@Override
	public boolean isAlwaysDirtyChecked() {
		return false;
	}

	@Override
	public boolean isEmbeddedInXML() {
		return false;
	}

	@Override
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException("any types do not have a unique referenced persister");
	}

	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException("any types do not have a unique referenced persister");
	}

	@Override
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOnCondition(
			String alias,
			SessionFactoryImplementor factory,
			Map enabledFilters,
			Set<String> treatAsDeclarations) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Used to externalize discrimination per a given identifier.  For example, when writing to
	 * second level cache we write the discrimination resolved concrete type for each entity written.
	 */
	public static final class ObjectTypeCacheEntry implements Serializable {
		final String entityName;
		final Serializable id;

		ObjectTypeCacheEntry(String entityName, Serializable id) {
			this.entityName = entityName;
			this.id = id;
		}
	}
}
