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
package org.hibernate.test.annotations.enumerated.mapkey;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Dmitry Spikhalskiy
 * @author Steve Ebersole
 */
@Entity
@Table( name = "USER_TABLE" )
public class User {
	@javax.persistence.Id
	@javax.persistence.GeneratedValue(generator = "system-uuid")
	@org.hibernate.annotations.GenericGenerator(name = "system-uuid", strategy = "uuid2")
	@javax.persistence.Column(name = "id", unique = true)
	private java.lang.String id;

	@MapKeyEnumerated( EnumType.STRING )
	@MapKeyColumn(name = "social_network")
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Map<SocialNetwork, SocialNetworkProfile> socialNetworkProfiles = new EnumMap<SocialNetwork, SocialNetworkProfile>(SocialNetwork.class);

	protected User() {
	}

	public User(SocialNetwork sn, String socialNetworkId) {
		SocialNetworkProfile profile = new SocialNetworkProfile(this, sn, socialNetworkId);
		socialNetworkProfiles.put(sn, profile);
	}

	public SocialNetworkProfile getSocialNetworkProfile(SocialNetwork socialNetwork) {
		return socialNetworkProfiles.get(socialNetwork);
	}

	public String getId() {
		return id;
	}
}
