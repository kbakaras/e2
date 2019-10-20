package ru.kbakaras.e2.message;

import org.dom4j.Element;

/**
 * Класс-обёртка для xml-сообщения с обновлением.
 */
public class E2Update extends E2Payload implements E2XmlProducer {

    public E2Update() {
        super(Use.createRoot("updateRequest", E2.NS));
    }
    public E2Update(Element xml) {
        super(xml);
    }
    public E2Update(String xmlStr) {
        super(Use.parse4Root(xmlStr));
    }


    @Override
    public Element xml() {
        reorderEntitiesAndStates();
        return xml;
    }

}