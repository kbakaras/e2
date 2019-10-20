package ru.kbakaras.e2.message;

import org.dom4j.Element;

public class E2Row {
    public final E2Attributes attributes;
    private Element xml;

    public E2Row(Element xml) {
        this.xml = xml;
        this.attributes = new E2Attributes(xml);
    }
}