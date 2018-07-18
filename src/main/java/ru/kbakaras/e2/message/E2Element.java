package ru.kbakaras.e2.message;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.SimpleVariableContext;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.Optional;

public class E2Element {
    public final E2Entity parent;
    public final E2Attributes attributes;

    private Element xml;

    public E2Element(Element xml, E2Entity parent) {
        this.xml        = xml;
        this.parent     = parent;
        this.attributes = new E2Attributes(xml);
    }

    public String entityName() {
        return xml.getParent().attributeValue(E2.ENTITY_NAME);
    }

    public String getUid() {
        return xml.attributeValue(E2.ELEMENT_UID);
    }

    public boolean isChanged() {
        return Boolean.parseBoolean(xml.attributeValue(E2.CHANGED));
    }


    public E2Table tableOrNull(String tableName) {
        XPath expr = tableXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue(E2.TABLE_NAME, tableName);
        Element table = (Element) expr.selectSingleNode(xml);
        return table != null ? new E2Table(table, this) : null;
    }

    public Optional<E2Table> table(String tableName) {
        return Optional.ofNullable(tableOrNull(tableName));
    }


    public E2Element setUid(String uid) {
        xml.addAttribute(E2.ELEMENT_UID, uid);
        return this;
    }

    public E2Element setChanged(boolean changed) {
        xml.addAttribute(E2.CHANGED, changed ? "true" : null);
        return this;
    }

    public E2Table addTable(String tableName) {
        return new E2Table(xml.addElement(E2.TABLE), this).setName(tableName);
    }

    public E2Reference asReference() {
        return new E2Reference(parent.entityName(), getUid());
    }

    @Override
    public int hashCode() {
        return getUid().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj instanceof E2Element) {
            return getUid().equals(((E2Element) obj).getUid());
        }

        return false;
    }

    private static Lazy<XPath> tableXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:table[@tableName=$tableName]",
                new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });
}