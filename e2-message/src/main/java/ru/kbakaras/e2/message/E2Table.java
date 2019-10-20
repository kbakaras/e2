package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class E2Table implements Iterable<E2Row> {
    public final E2Element parent;
    private Element xml;

    public E2Table(Element xml, E2Element parent) {
        this.xml = xml;
        this.parent = parent;
    }

    public String tableName() {
        return xml.attributeValue(E2.TABLE_NAME);
    }

    public E2AttributeUse use() {
        String use = xml.attributeValue(E2.USE);

        if (use == null || E2.USE_Always.equals(use)) {
            return E2AttributeUse.Always;
        } else if (E2.USE_Create.equals(use)) {
            return E2AttributeUse.Create;

        } else {
            throw new E2Exception4Read("Invalid value for attribute's flag `use`: " + use);
        }
    }

    public E2Row rowOrNull(int index) {
        List<Element> rowElements = xml.elements(E2.ROW);
        return index < rowElements.size() ? new E2Row(rowElements.get(index)) : null;
    }
    public Optional<E2Row> row(int index) {
        return Optional.ofNullable(rowOrNull(index));
    }

    @Override
    public Iterator<E2Row> iterator() {
        return new Iterator<E2Row>() {
            private Iterator<Element> iterator = xml.elements(E2.ROW).iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E2Row next() {
                return new E2Row(iterator.next());
            }
        };

    }


    public E2Table setTableName(String name) {
        this.xml.addAttribute(E2.TABLE_NAME, name);
        return this;
    }

    public E2Table setUse(E2AttributeUse use) {
        String useValue = null;

        if (use == E2AttributeUse.Create) {
            useValue = E2.USE_Create;
        }

        xml.addAttribute(E2.USE, useValue);
        return this;
    }

    public E2Row addRow() {
        return new E2Row(xml.addElement(E2.ROW));
    }
}