package ru.kbakaras.e2.message;

import org.dom4j.Element;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс-обёртка для xml-сообщения с ответом на запрос к одной системе. Объекты этого класса
 * агрегируются в итоговое сообщение-ответ, обёртка для которого реализована классом {@link E2Response}.
 */
@SuppressWarnings("WeakerAccess")
public class E2SystemResponse extends E2Payload {

    public final E2Response parent;

    E2SystemResponse(Element xml, E2Response parent) {
        super(xml);
        this.parent = parent;
    }


    public E2SystemError addSystemError() {
        return new E2SystemError(this);
    }


    public List<E2SystemError> errors() {
        return xml.elements(E2.ERROR).stream()
                .map(xml -> new E2SystemError(xml, E2SystemResponse.this))
                .collect(Collectors.toList());
    }

}