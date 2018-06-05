package ru.kbakaras.e2.message;

import org.butu.sugar.lazy.Lazy;
import org.dom4j.Element;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Обёртка для удобства работы с запросами на получение элементов.
 */
public class E2Request {
    private Element xml;

    private Lazy<E2Payload> lContext = Lazy.of(() -> new E2Payload(
            Optional.ofNullable(xml.element(E2.CONTEXT))
                    .orElse(xml.addElement(E2.CONTEXT))
    ));

    public E2Request(Element xml) {
        this.xml = xml;
    }

    public E2Request(String requestType) {
        this.xml = Use.createRoot(requestType + "Request", E2.NS);
    }

    public UUID sourceSystemUid() {
        return UUID.fromString(xml.attributeValue("systemUid"));
    }

    public String sourceSystemName() {
        return xml.attributeValue("systemName");
    }

    public UUID[] destinationSystemUids() {
        return xml.elements(E2.SYSTEM).stream()
                .map(element -> UUID.fromString(element.attributeValue("systemUid")))
                .toArray(UUID[]::new);
    }

    public String requestType() {
        String rootName = xml.getName();
        if ("listRequest".equals(rootName)) {
            return "list";
        } else if ("elementRequest".equals(rootName)) {
            return "element";
        } else {
            throw new IllegalArgumentException("Request type unknown: <" + rootName + ">!");
        }
    }

    public List<E2ReferenceRequest> references() {
        return xml.elements("reference").stream()
                .map(E2ReferenceRequest::new)
                .collect(Collectors.toList());

    }

    public List<E2EntityRequest> entities() {
        return xml.elements("entity").stream()
                .map(E2EntityRequest::new)
                .collect(Collectors.toList());

    }

    public Optional<E2Payload> context() {
        return Optional.ofNullable(xml.element("context")).map(E2Payload::new);
    }


    public E2Request setSourceSystem(String sourceSystemUid, String sourceSystemName) {
        this.xml.addAttribute("systemUid", sourceSystemUid);
        this.xml.addAttribute("systemName", sourceSystemName);
        return this;
    }

    public E2Request addDestinationSystem(String destinationSystemUid) {
        xml.addElement(E2.SYSTEM).addAttribute(E2.SYSTEM_UID, destinationSystemUid);
        return this;
    }

    public E2ReferenceRequest addReferenceRequest(String entityName, String elementUid) {
        return new E2ReferenceRequest(xml.addElement(E2.REFERENCE))
                .setEntityName(entityName).setElementUid(elementUid);
    }

    public E2EntityRequest addEntityRequest(String entityName) {
        return new E2EntityRequest(xml.addElement(E2.ENTITY).addAttribute(E2.ENTITY_NAME, entityName));
    }

    public E2Payload createContext() {
        return lContext.get();
    }

    public Element xml() {
        return xml;
    }
}