package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.conversion.context.Conversion4Element;
import ru.kbakaras.e2.conversion.context.ConversionContext;

public class ConversionCopy extends Conversion {
    private Conversion4Element copyProducer;

    public ConversionCopy(String destinationEntityName) {
        super(destinationEntityName);

        copyProducer = new Conversion4Element();
        copyProducer.attributes.copyUntouched();
        copyProducer.tables.copyUntouched();
    }

    @Override
    public void convertElement(ConversionContext context) {
        context.addResult(copyProducer);
    }
}