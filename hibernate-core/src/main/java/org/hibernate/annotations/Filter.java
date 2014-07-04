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
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Add filters to an entity or a target entity of a collection.
 *
 * @author Emmanuel Bernard
 * @author Matthew Inger
 * @author Magnus Sandberg
 * @author Rob Worsnop
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Filter {
	/**
	 * The filter name.
	 */
	String name();

	/**
	 * The filter condition.  If empty, the default condition from the correspondingly named {@link FilterDef} is used.
	 */
	String condition() default "";

	/**
	 * If true, automatically determine all points within the condition fragment that an alias should be injected.
	 * Otherwise, injection will only replace instances of explicit "{alias}" instances or
	 * @SqlFragmentAlias descriptors.
	 */
	boolean deduceAliasInjectionPoints() default true;

	/**
	 * The alias descriptors for injection.
	 */
	SqlFragmentAlias[] aliases() default {};
}
