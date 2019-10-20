package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.math.BigDecimal;
import java.util.Optional;

public class E2Attribute implements E2Referring<E2Attribute> {
    private Element xml;

    public E2Attribute(Element xml) {
        this.xml = xml;
    }

    public String attributeName() {
        return xml.attributeValue(E2.ATTRIBUTE_NAME);
    }

    public boolean isReference() {
        return xml.element(E2.REFERENCE) != null;
    }

    public boolean isValue() {
        return xml.element(E2.VALUE) != null;
    }

    public boolean isId() {
        return Boolean.parseBoolean(xml.attributeValue(E2.ID));
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


    public E2AttributeValue attributeValue() {
        return isReference() ? reference() : value();
    }

    public E2Reference reference() {
        return Optional.ofNullable(xml.element(E2.REFERENCE))
                .map(reference -> new E2Reference(
                        reference.attributeValue(E2.ENTITY_NAME),
                        reference.attributeValue(E2.ELEMENT_UID)))
                .orElseThrow(() -> new E2Exception4Read("Attribute string is not a reference!"));
    }

    public E2Scalar value() {
        return Optional.ofNullable(xml.element(E2.VALUE))
                .map(value -> new E2Scalar(value.getText()))
                .orElseThrow(() -> new E2Exception4Read("Attribute does not contain string!"));
    }


    public E2Attribute setAttributeName(String attributeName) {
        xml.addAttribute(E2.ATTRIBUTE_NAME, attributeName);
        return this;
    }

    public E2Attribute setId(boolean isId) {
        xml.addAttribute(E2.ID, isId ? "true" : null);
        return this;
    }

    public E2Attribute setUse(E2AttributeUse use) {
        String useValue = null;

        if (use == E2AttributeUse.Create) {
            useValue = E2.USE_Create;
        }

        xml.addAttribute(E2.USE, useValue);
        return this;
    }


    @Override
    public E2Attribute setValue(String value) {
        if (isReference()) {
            throw new E2Exception4Write("Attribute already has reference-value!");
        }

        Optional.ofNullable(xml.element(E2.VALUE))
                .orElseGet(() -> xml.addElement(E2.VALUE))
                .setText(value);

        return this;
    }

    @Override
    public E2Attribute setReference(String entityName, String elementUid) {
        if (isValue()) {
            throw new E2Exception4Write("Attribute already has simple value!");
        }

        Optional.ofNullable(xml.element(E2.REFERENCE))
                .orElseGet(() -> xml.addElement(E2.REFERENCE))
                .addAttribute(E2.ENTITY_NAME, entityName)
                .addAttribute(E2.ELEMENT_UID, elementUid);

        return this;
    }

    public E2Attribute setReference(E2Reference reference) {
        setReference(reference.entityName, reference.elementUid);
        return this;
    }

    public static Integer mapInteger(E2Attribute attribute) {
        return new Integer(attribute.value().string());
    }

    public static BigDecimal mapDecimal(E2Attribute attribute) {
        return new BigDecimal(attribute.value().string());
    }

    public static String mapString(E2Attribute attribute) {
        return attribute.value().string();
    }
}