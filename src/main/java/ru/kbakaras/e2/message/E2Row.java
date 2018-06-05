package ru.kbakaras.e2.message;

import org.dom4j.Element;

public class E2Row {
    private Element xml;

    public E2Row(Element xml) {
        this.xml = xml;
    }

    public E2Attribute addAttribute(String attributeName) {
        return new E2Attribute(xml.addElement(E2.ATTRIBUTE))
                .setAttributeName(attributeName);
    }
}
