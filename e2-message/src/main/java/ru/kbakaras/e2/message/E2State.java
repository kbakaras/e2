package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.Iterator;

public class E2State implements Iterable<E2Row> {
    private Element xml;

    public E2State(Element xml) {
        this.xml = xml;
    }

    public String stateName() {
        return xml.attributeValue(E2.STATE_NAME);
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


    public E2State setStateName(String name) {
        this.xml.addAttribute(E2.STATE_NAME, name);
        return this;
    }

    public E2Row addRow() {
        return new E2Row(xml.addElement(E2.ROW));
    }
}