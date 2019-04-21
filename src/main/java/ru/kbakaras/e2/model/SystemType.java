package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

public abstract class SystemType extends ProperEntity {
    public final String name;

    public SystemType(String name) {
        this.name = name;
    }
}