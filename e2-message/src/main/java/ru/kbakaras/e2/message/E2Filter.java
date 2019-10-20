package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.Optional;

public class E2Filter implements E2Referring<E2Filter> {
    private Element xml;

    public E2Filter(Element xml) {
        this.xml = xml;
    }

    public String attributeName() {
        return xml.attributeValue(E2.ATTRIBUTE_NAME);
    }

    public String condition() {
        return xml.attributeValue(E2.CONDITION);
    }

    public boolean isReference() {
        return xml.element(E2.REFERENCE) != null;
    }

    public boolean isValue() {
        return xml.element(E2.VALUE) != null;
    }

    public E2AttributeValue attributeValue() {
        return isReference() ? reference() : value();
    }

    public E2Reference reference() {
        return Optional.ofNullable(xml.element(E2.REFERENCE))
                .map(reference -> new E2Reference(
                        reference.attributeValue(E2.ENTITY_NAME),
                        reference.attributeValue(E2.ELEMENT_UID)))
                .orElseThrow(() -> new E2Exception4Read("Filter value is not a reference!"));
    }

    public E2Scalar value() {
        return Optional.ofNullable(xml.element(E2.VALUE))
                .map(value -> new E2Scalar(value.getText()))
                .orElseThrow(() -> new E2Exception4Read("Filter does not contain string!"));
    }


    public E2Filter setAttributeName(String attributeName) {
        xml.addAttribute(E2.ATTRIBUTE_NAME, attributeName);
        return this;
    }

    public E2Filter setCondition(String condition) {
        xml.addAttribute(E2.CONDITION, condition);
        return this;
    }

    @Override
    public E2Filter setValue(String value) {
        if (isReference()) {
            throw new E2Exception4Write("Filter already has reference-value!");
        }

        Optional.ofNullable(xml.element(E2.VALUE))
                .orElseGet(() -> xml.addElement(E2.VALUE))
                .setText(value);

        return this;
    }

    @Override
    public E2Filter setReference(String entityName, String elementUid) {
        if (isValue()) {
            throw new E2Exception4Write("Filter already has simple value!");
        }

        Optional.ofNullable(xml.element(E2.REFERENCE))
                .orElseGet(() -> xml.addElement(E2.REFERENCE))
                .addAttribute(E2.ENTITY_NAME, entityName)
                .addAttribute(E2.ELEMENT_UID, elementUid);

        return this;
    }
}