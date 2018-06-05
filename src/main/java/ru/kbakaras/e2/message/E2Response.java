package ru.kbakaras.e2.message;

import org.butu.sugar.lazy.Lazy;
import org.dom4j.Element;

import java.util.Optional;

public class E2Response {
    private Element xml;

    public E2Response(Element xml) {
        this.xml = xml;
    }

    /**
     * Создание сообщения-ответа указанного типа. Этот конструктор
     * создаст пустой корневой xml-узел.
     * @param responseType Тип сообщения.
     */
    public E2Response(String responseType) {
        this.xml = Use.createRoot(responseType + "Response", E2.NS);
    }

    public E2SystemResponse systemResponse() {
        return Optional.ofNullable(xml.element("systemResponse"))
                .map(xml -> new E2SystemResponse(xml, this)).orElse(null);
    }

    public final Lazy<String> responseType = Lazy.of(() -> {
        String rootName = xml.getName();
        if ("listResponse".equals(rootName)) {
            return "list";
        } else if ("elementResponse".equals(rootName)) {
            return "element";
        } else {
            throw new IllegalArgumentException("Response type unknown: <" + rootName + ">!");
        }
    });


    public E2SystemResponse addSystemResponse(String responseSystemUid, String responseSystemName) {
        return new E2SystemResponse(xml.addElement("systemResponse"), this)
                .setResponseSystemUid(responseSystemUid)
                .setResponseSystemName(responseSystemName);
    }

    public void addSystemError(String responseSystemUid, String responseSystemName, Element errorResponse) {
        xml.addElement("systemResponse")
                .addAttribute("systemUid",  responseSystemUid)
                .addAttribute("systemName", responseSystemName)
                .add(errorResponse.detach());
    }

    public Element xml() {
        return xml;
    }
}