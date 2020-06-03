package com.company.gaef.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@NamePattern("%s|title")
@Table(name = "GAEF_BOOK")
@Entity(name = "gaef$Book")
public class Book extends StandardEntity {
    private static final long serialVersionUID = 623762775218457948L;

    @Column(name = "TITLE")
    protected String title;

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }


}
