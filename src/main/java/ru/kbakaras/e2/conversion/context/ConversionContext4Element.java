package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Element;

public class ConversionContext4Element {
    public final E2Element sourceElement;
    public final E2Element destinationElement;

    public ConversionContext4Element(E2Element sourceElement, E2Element destinationElement) {
        this.sourceElement = sourceElement;
        this.destinationElement = destinationElement;
    }
}
