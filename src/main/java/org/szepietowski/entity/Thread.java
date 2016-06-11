package org.szepietowski.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Thread extends BaseEntity {
    @Column
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
