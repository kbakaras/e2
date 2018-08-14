package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Attributes;

public class ConversionContext4Producer {
    public final E2Attributes sourceAttributes;
    public final E2Attributes destinationAttributes;

    public ConversionContext4Producer(E2Attributes sourceAttributes, E2Attributes destinationAttributes) {
        this.sourceAttributes = sourceAttributes;
        this.destinationAttributes = destinationAttributes;
    }
}
