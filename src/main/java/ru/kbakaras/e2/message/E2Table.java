package ru.kbakaras.e2.message;

import org.dom4j.Element;

public class E2Table {
    private Element xml;

    public E2Table(Element xml, String tableName) {
        this.xml = xml;
        this.xml.addAttribute(E2.TABLE_NAME, tableName);
    }

    public E2Row addRow() {
        return new E2Row(xml.addElement(E2.ROW));
    }
}
