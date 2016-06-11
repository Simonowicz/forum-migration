package org.szepietowski.entity;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Forum extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "parentForum")
    private Forum parentForum;

    @OneToMany(mappedBy = "parentForum")
    private Set<Forum> childrenForums;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Forum getParentForum() {
        return parentForum;
    }

    public void setParentForum(Forum parentForum) {
        this.parentForum = parentForum;
    }

    public Set<Forum> getChildrenForums() {
        return childrenForums;
    }

    public void setChildrenForums(Set<Forum> childrenForums) {
        this.childrenForums = childrenForums;
    }

    @Override
    public String toString() {
        return "Forum{" +
                "id=" + getId() +
                "description='" + description + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
