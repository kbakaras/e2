package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.message.E2Attribute;

public class ConversionCopy extends Conversion {

    public ConversionCopy(String destinationEntityName) {
        super(destinationEntityName);
    }

    @Override
    public void convertElement(ConversionContext context) {
        context.addResult().ok()
                .copyAllAttributes(this::filter);
    }

    protected boolean filter(E2Attribute reader) {
        return true;
    }
}