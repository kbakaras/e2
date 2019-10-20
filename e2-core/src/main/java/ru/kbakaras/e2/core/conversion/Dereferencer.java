package ru.kbakaras.e2.core.conversion;

import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Payload;

import java.util.Optional;

public class Dereferencer {
    private E2Payload payload;
    private E2Element element;

    public Dereferencer(E2Payload payload, E2Element element) {
        this.payload = payload;
        this.element = element;
    }

    public Optional<E2Element> dereference(String... attributeNames) {
        Optional<E2Element> result = Optional.ofNullable(element);

        for (String attributeName: attributeNames) {
            result = result.flatMap(element -> element.attributes.get(attributeName))
                    .map(E2Attribute::reference)
                    .flatMap(payload::referencedElement);
        }

        return result;
    }
}