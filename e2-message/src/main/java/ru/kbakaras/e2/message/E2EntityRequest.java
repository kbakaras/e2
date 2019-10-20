package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.List;
import java.util.stream.Collectors;

public class E2EntityRequest {
    private Element entityRequestElement;

    public E2EntityRequest(Element entityRequestElement) {
        this.entityRequestElement = entityRequestElement;
    }

    public String entityName() {
        return entityRequestElement.attributeValue(E2.ENTITY_NAME);
    }

    public List<E2Filter> filters() {
        return entityRequestElement.elements("filter").stream()
                .map(E2Filter::new)
                .collect(Collectors.toList());
    }


    public E2Filter addFilter(String attributeName) {
        return new E2Filter(entityRequestElement.addElement("filter"))
                .setAttributeName(attributeName);
    }
}