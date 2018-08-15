package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Attributes;

public class ConversionContext4Producer {
    public final ConversionContext4Element parent;
    public final E2Attributes sourceAttributes;
    public final E2Attributes destinationAttributes;

    ConversionContext4Producer(ConversionContext4Element parent, E2Attributes sourceAttributes, E2Attributes destinationAttributes) {
        this.parent = parent;
        this.sourceAttributes = sourceAttributes;
        this.destinationAttributes = destinationAttributes;
    }
}
