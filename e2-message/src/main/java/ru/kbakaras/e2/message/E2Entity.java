package ru.kbakaras.e2.message;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.jaxen.SimpleVariableContext;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public class E2Entity {

    private Element xml;


    public E2Entity(Element xml) {
        this.xml = xml;
    }


    public String entityName() {
        return xml.attributeValue(E2.ENTITY_NAME);
    }

    public List<E2Element> elementsChanged() {
        return changedXPath.get().selectNodes(xml).stream()
                .map(node -> new E2Element((Element) node, this))
                .collect(Collectors.toList());
    }

    public Optional<E2Element> element(String elementUid) {
        XPath expr = elementXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue("elementUid", elementUid);

        return Optional.ofNullable((Element) expr.selectSingleNode(xml))
                .map(node -> new E2Element(node, this));
    }

    public List<E2Element> elementsByAttribute(String attributeName, E2Reference reference) {
        XPath expr = attributeXPath.get();

        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue(E2.ATTRIBUTE_NAME, attributeName);
        vc.setVariableValue(E2.ENTITY_NAME, reference.entityName);
        vc.setVariableValue(E2.ELEMENT_UID, reference.elementUid);

        return expr.selectNodes(xml).stream()
                .map(node -> new E2Element((Element) node, this))
                .collect(Collectors.toList());
    }

    public ElementFinder find() {
        return new ElementFinder();
    }


    /**
     * Создаёт новый элемент в данной сущности, копирует признак изменённости и уникальный идентификатор
     * из переданного в параметре исходного элемента.
     * @param source Исходный элемент.
     * @return Обёртка для нового элемента.
     */
    public E2Element addElement(E2Element source) {
        return new E2Element(xml.addElement("element"), this)
                .setChanged(source.isChanged())
                .setUid(source.getUid());
    }

    /**
     * Создаёт новый элемент в данной сущности, назначет ему указанный uid.
     * При назначении uid не выполняется никаких проверок на уникальность.
     * @return Обёртка для нового элемента
     */
    public E2Element addElement(String uid) {
        return new E2Element(xml.addElement("element"), this).setUid(uid);
    }


    private static Lazy<XPath> elementXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:element[@elementUid=$elementUid]",
                new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });

    private static Lazy<XPath> changedXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:element[@changed='true']");
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });

    private static Lazy<XPath> attributeXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:element[e2:attribute[@attributeName=$attributeName]/e2:reference[@entityName=$entityName and @elementUid=$elementUid]]",
                new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });


    public class ElementFinder {
        private String expr = "e2:element";

        public ElementFinder by(String attributeName, String attributeValue) {
            expr += "[e2:attribute[@attributeName='" + attributeName + "']" +
                    "/e2:value[text()='" + attributeValue + "']]";
            return this;
        }

        public ElementFinder by(String attributeName, E2Reference reference) {
            expr += "[e2:attribute[@attributeName='" + attributeName + "']" +
                    "/e2:reference[@entityName='" + reference.entityName + "' and @elementUid='" + reference.elementUid + "']]";
            return this;
        }


        private XPath createXPath() {
            XPath xPath = DocumentFactory.getInstance().createXPath(expr);
            xPath.setNamespaceURIs(E2.E2MAP);
            return xPath;
        }


        public Optional<E2Element> single() {
            List<Node> list = createXPath().selectNodes(xml);
            if (list.size() == 1) {
                return Optional.of(new E2Element(
                        (Element) list.get(0),
                        E2Entity.this));

            } else if (list.isEmpty()) {
                return Optional.empty();

            } else {
                throw new E2Exception4Read("Found multiple elements in single-element request!");
            }
        }

        public List<E2Element> list() {
            return createXPath().selectNodes(xml).stream()
                    .map(node -> new E2Element((Element) node, E2Entity.this))
                    .collect(Collectors.toList());
        }
    }

}