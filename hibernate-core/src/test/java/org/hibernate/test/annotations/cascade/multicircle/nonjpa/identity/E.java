/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class E extends AbstractEntity {
    private static final long serialVersionUID = 1226955558L;

	@javax.persistence.OneToMany(mappedBy = "e")
	private java.util.Set<D> dCollection = new java.util.HashSet<D>();

	@javax.persistence.ManyToOne(optional = true)
	private F f;

	public java.util.Set<D> getDCollection() {
		return dCollection;
	}
	public void setDCollection(java.util.Set<D> dCollection) {
		this.dCollection = dCollection;
	}

    public F getF() {
        return f;
    }
    public void setF(F parameter) {
        this.f = parameter;
    }
}
