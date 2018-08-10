package ru.kbakaras.e2.conversion;

public class ConversionCopy extends Conversion {

    public ConversionCopy(String destinationEntityName) {
        super(destinationEntityName);
    }

    @Override
    public void convertElement(ConversionContext context) {
        context.addResult().ok().copyUntouched();
    }
}