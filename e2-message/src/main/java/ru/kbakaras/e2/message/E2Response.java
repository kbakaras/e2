package ru.kbakaras.e2.message;

import org.dom4j.Element;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Класс-обёртка для сообщения, агрегирующего ответы на запрос, полученные от нескольких систем.
 */
public class E2Response implements E2XmlProducer {

    private Element xml;

    private Lazy<List<E2SystemResponse>> responses = Lazy.of(ArrayList::new);


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
        E2SystemResponse response = new E2SystemResponse(xml.addElement("systemResponse"), this)
                .setSystemUid(responseSystemUid)
                .setSystemName(responseSystemName);
        responses.get().add(response);
        return response;
    }


    @Override
    public Element xml() {
        if (!responses.isVirgin()) {
            responses.get().forEach(E2SystemResponse::reorderEntitiesAndStates);
        }
        return xml;
    }

}