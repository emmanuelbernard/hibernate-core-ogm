//$Id$
package org.hibernate.jpa.test.cascade;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.OrderBy;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Troop implements Serializable {
	private Integer id;
	private String name;
	private Set<Soldier> soldiers;

	@OneToMany(mappedBy = "troop", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
	@OrderBy(clause = "name desc")
	public Set<Soldier> getSoldiers() {
		return soldiers;
	}

	public void setSoldiers(Set<Soldier> soldiers) {
		this.soldiers = soldiers;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addSoldier(Soldier s) {
		if ( soldiers == null ) soldiers = new HashSet<Soldier>();
		soldiers.add( s );
		s.setTroop( this );
	}
}
