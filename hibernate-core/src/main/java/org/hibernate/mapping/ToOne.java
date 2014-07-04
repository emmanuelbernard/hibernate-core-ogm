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
package org.hibernate.mapping;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.Mappings;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.Type;

/**
 * A simple-point association (ie. a reference to another entity).
 * @author Gavin King
 */
public abstract class ToOne extends SimpleValue implements Fetchable {

	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private boolean embedded;
	private boolean lazy = true;
	protected boolean unwrapProxy;
	protected boolean referenceToPrimaryKey = true;

	protected ToOne(Mappings mappings, Table table) {
		super( mappings, table );
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public abstract void createForeignKey() throws MappingException;
	public abstract Type getType() throws MappingException;

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? 
				null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName)
	throws MappingException {
		if (referencedEntityName==null) {
			referencedEntityName = ReflectHelper.reflectedPropertyClass( className, propertyName ).getName();
		}
	}

	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	/**
	 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public boolean isEmbedded() {
		return embedded;
	}

	/**
	 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		if (referencedEntityName==null) {
			throw new MappingException("association must specify the referenced entity");
		}
		return super.isValid( mapping );
	}

	public boolean isLazy() {
		return lazy;
	}
	
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	public void setUnwrapProxy(boolean unwrapProxy) {
		this.unwrapProxy = unwrapProxy;
	}

	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

	public void setReferenceToPrimaryKey(boolean referenceToPrimaryKey) {
		this.referenceToPrimaryKey = referenceToPrimaryKey;
	}
	
}
