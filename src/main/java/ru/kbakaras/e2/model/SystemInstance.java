package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "system_instance")
public abstract class SystemInstance extends ProperEntity {

    private String name;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getName();
    }

}