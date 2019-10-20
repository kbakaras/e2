package ru.kbakaras.e2.message;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.SimpleVariableContext;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс, инкапсулирующий работу с атрибутами элементов и строк таблиц.
 * Не является самостоятельным, используется в классах {@link E2Element}
 * и {@link E2Row}.
 */
public class E2Attributes {
    private Element xml;

    E2Attributes(Element xml) {
        this.xml = xml;
    }

    public Stream<E2Attribute> stream() {
        return xml.elements("attribute").stream()
                .map(E2Attribute::new);
    }

    public List<E2Attribute> list() {
        return stream().collect(Collectors.toList());
    }

    public E2Attribute getNullable(String attributeName) {
        XPath expr = attributeXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue(E2.ATTRIBUTE_NAME, attributeName);
        Element attribute = (Element) expr.selectSingleNode(xml);
        return attribute != null ? new E2Attribute(attribute) : null;
    }

    public Optional<E2Attribute> get(String attributeName) {
        return Optional.ofNullable(getNullable(attributeName));
    }

    /**
     * @return true, если атрибут существует и содержит значение "true". Во всех остальных
     * случаях - false.
     */
    public boolean is(String attributeName) {
        return get(attributeName)
                .map(attr -> "true".equals(attr.value().string()))
                .orElse(false);
    }

    /**
     *
     * @param attributeName
     * @param value
     * @return true, если атрибут существует и содержит строку, эквивалентную переданной
     * в параметре value. Во всех остальных случаях - false.
     */
    public boolean isEqual(String attributeName, String value) {
        return get(attributeName)
                .map(attr -> attr.value().string().equals(value))
                .orElse(false);
    }

    public E2Attribute add(String attributeName) {
        return new E2Attribute(xml.addElement(E2.ATTRIBUTE))
                .setAttributeName(attributeName);
    }


    private static Lazy<XPath> attributeXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:attribute[@attributeName=$attributeName]",
                new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });
}