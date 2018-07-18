package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.List;
import java.util.Optional;

public class E2Table {
    public final E2Element parent;
    private Element xml;

    public E2Table(Element xml, E2Element parent) {
        this.xml = xml;
        this.parent = parent;
    }

    public E2Row rowOrNull(int index) {
        List<Element> rowElements = xml.elements("row");
        return index < rowElements.size() ? new E2Row(rowElements.get(index)) : null;
    }
    public Optional<E2Row> row(int index) {
        return Optional.ofNullable(rowOrNull(index));
    }

    public E2Table setName(String name) {
        this.xml.addAttribute(E2.TABLE_NAME, name);
        return this;
    }

    public E2Row addRow() {
        return new E2Row(xml.addElement(E2.ROW));
    }
}
