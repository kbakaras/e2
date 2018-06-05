package ru.kbakaras.e2.message;

import org.dom4j.Element;

public class E2ReferenceRequest {
    private Element xml;

    public E2ReferenceRequest(Element xml) {
        this.xml = xml;
    }

    public String entityName() {
        return xml.attributeValue(E2.ENTITY_NAME);
    }

    public String elementUid() {
        return xml.attributeValue(E2.ELEMENT_UID);
    }

    public E2ReferenceRequest setEntityName(String entityName) {
        xml.addAttribute(E2.ENTITY_NAME, entityName);
        return this;
    }

    public E2ReferenceRequest setElementUid(String elementUid) {
        xml.addAttribute(E2.ELEMENT_UID, elementUid);
        return this;
    }
}