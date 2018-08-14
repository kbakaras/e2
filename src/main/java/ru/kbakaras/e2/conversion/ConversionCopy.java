package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.conversion.context.ConversionContext;

public class ConversionCopy extends Conversion {

    public ConversionCopy(String destinationEntityName) {
        super(destinationEntityName);
    }

    @Override
    public void convertElement(ConversionContext context) {
        context.addResult().ok().copyUntouched();
    }
}