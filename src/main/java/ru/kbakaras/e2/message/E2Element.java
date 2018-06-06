package ru.kbakaras.e2.message;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.SimpleVariableContext;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class E2Element {
    public final E2Entity parent;

    private Element xml;

    public E2Element(Element xml, E2Entity parent) {
        this.xml    = xml;
        this.parent = parent;
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

    public List<E2Attribute> attributes() {
        return xml.elements("attribute").stream()
                .map(E2Attribute::new)
                .collect(Collectors.toList());
    }

    public E2Attribute attributeOrNull(String attributeName) {
        XPath expr = attributeXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue(E2.ATTRIBUTE_NAME, attributeName);
        Element attribute = (Element) expr.selectSingleNode(xml);
        return attribute != null ? new E2Attribute(attribute) : null;
    }

    public Optional<E2Attribute> attribute(String attributeName) {
        return Optional.ofNullable(attributeOrNull(attributeName));
    }

    public boolean attributeBoolean(String attributeName) {
        return attribute(attributeName)
                .map(attr -> "true".equals(attr.value().string()))
                .orElse(false);
    }



    public E2Element setUid(String uid) {
        xml.addAttribute(E2.ELEMENT_UID, uid);
        return this;
    }

    public E2Element setChanged(boolean changed) {
        xml.addAttribute(E2.CHANGED, changed ? "true" : null);
        return this;
    }

    public E2Attribute addAttribute(String attributeName) {
        return new E2Attribute(xml.addElement(E2.ATTRIBUTE))
                .setAttributeName(attributeName);
    }
    public E2Attribute addSynthAttribute(String attributeName) {
        return new E2Attribute(xml.addElement(E2.ATTRIBUTE))
                .setAttributeName(attributeName)
                .setSynth(true);
    }

    public E2Table addTable(String tableName) {
        return new E2Table(xml.addElement(E2.TABLE), tableName);
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

    public static Lazy<XPath> attributeXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:attribute[@attributeName=$attributeName]",
                new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });
}