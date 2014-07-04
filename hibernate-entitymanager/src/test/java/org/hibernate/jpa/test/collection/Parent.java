package org.hibernate.jpa.test.collection;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

@Entity
public class Parent {

    private Integer id;
    private Set<Child> children = new HashSet<Child>();
    private int nrOfChildren;
    
    public Parent() {
        
    }
    
    @Id
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    @OneToMany(mappedBy="daddy", fetch=FetchType.EAGER, cascade=CascadeType.ALL)
    public Set<Child> getChildren() {
        return children;
    }
    public void setChildren(Set<Child> children) {
        this.children = children;
    }
    
    @PostLoad
    public void postLoad() {
       nrOfChildren = children.size();
    }
    
    @Transient
    public int getNrOfChildren() {
        return nrOfChildren;
    }
}

