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

    public boolean isDeleted() {
        return Boolean.parseBoolean(xml.attributeValue(E2.DELETED));
    }

    public boolean isSynth() {
        return Boolean.parseBoolean(xml.attributeValue(E2.SYNTH));
    }

    public E2ElementUse use() {
        String use = xml.attributeValue(E2.USE);
        if (use == null || E2.USE_Load.equals(use)) {
            return E2ElementUse.Load;
        } else if (E2.USE_Reference.equals(use)) {
            return E2ElementUse.Reference;
        } else if (E2.USE_Update.equals(use)) {
            return E2ElementUse.Update;

        } else {
            throw new E2Exception4Read("Invalid value for element's flag `use`: " + use);
        }
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

    public List<E2Table> tables() {
        return xml.elements(E2.TABLE).stream()
                .map(xml -> new E2Table(xml, this))
                .collect(Collectors.toList());
    }


    public E2Element setUid(String uid) {
        xml.addAttribute(E2.ELEMENT_UID, uid);
        return this;
    }

    public E2Element setChanged(boolean changed) {
        xml.addAttribute(E2.CHANGED, changed ? "true" : null);
        return this;
    }

    public E2Element setDeleted(boolean deleted) {
        xml.addAttribute(E2.DELETED, deleted ? "true" : null);
        return this;
    }

    public E2Element setSynth(boolean synth) {
        xml.addAttribute(E2.SYNTH, synth ? "true" : null);
        return this;
    }

    public E2Element setUse(E2ElementUse use) {
        String useValue = null;

        if (use == E2ElementUse.Reference) {
            useValue = E2.USE_Reference;
        } else if (use == E2ElementUse.Update) {
            useValue = E2.USE_Update;
        }

        xml.addAttribute(E2.USE, useValue);
        return this;
    }


    public E2Table addTable(String tableName) {
        return new E2Table(xml.addElement(E2.TABLE), this).setTableName(tableName);
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

    @Override
    public String toString() {
        return String.format("%1$s(%2$s)", entityName(), getUid());
    }

    private static Lazy<XPath> tableXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:table[@tableName=$tableName]",
                new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });
}