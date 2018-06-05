package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.UUID;

public class E2SystemResponse extends E2Payload {
    public final E2Response parent;

    public E2SystemResponse(Element xml, E2Response parent) {
        super(xml);
        this.parent = parent;
    }

    public UUID responseSystemUid() {
        return UUID.fromString(xml.attributeValue(E2.SYSTEM_UID));
    }

    public String responseSystemName() {
        return xml.attributeValue(E2.SYSTEM_NAME);
    }


    public E2SystemResponse setResponseSystemUid(String systemUid) {
        xml.addAttribute(E2.SYSTEM_UID, systemUid);
        return this;
    }

    public E2SystemResponse setResponseSystemName(String systemName) {
        xml.addAttribute(E2.SYSTEM_NAME, systemName);
        return this;
    }
}