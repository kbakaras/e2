package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.conversion.Dereferencer;
import ru.kbakaras.e2.message.E2Attributes;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Payload;

import java.util.Optional;

public class ConversionContext4Producer {
    public final ConversionContext4Element parent;
    public final E2Attributes sourceAttributes;
    public final E2Attributes destinationAttributes;

    ConversionContext4Producer(ConversionContext4Element parent, E2Attributes sourceAttributes, E2Attributes destinationAttributes) {
        this.parent = parent;
        this.sourceAttributes = sourceAttributes;
        this.destinationAttributes = destinationAttributes;
    }

    /**
     * Shortcut to parent.parent.converter.input
     */
    public E2Payload input() {
        return parent.parent.converter.input;
    }

    /**
     * Shortcut to parent.parent.converter
     */
    public Converter4Payload converter() {
        return parent.parent.converter;
    }

    public Optional<E2Element> sourceDereference(String... attributeNames) {
        return new Dereferencer(parent.parent.converter.input, parent.sourceElement)
                .dereference(attributeNames);
    }
    public Optional<E2Element> destinationDereference(String... attributeNames) {
        return new Dereferencer(parent.parent.converter.output, parent.destinationElement)
                .dereference(attributeNames);
    }
}