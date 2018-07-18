package ru.kbakaras.e2.model;

import org.dom4j.Element;
import ru.kbakaras.e2.message.E2Response;
import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "system_type")
@DiscriminatorColumn(name = "did", discriminatorType = DiscriminatorType.STRING, length = 36)
public abstract class SystemType extends ProperEntity {
    private String name;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Element convertRequest(Element request) {
        return request;
    }
    public E2Response convertResponse(Element response) {
        return new E2Response(response);
    }
}