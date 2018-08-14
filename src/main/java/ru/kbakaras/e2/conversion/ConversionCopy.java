package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.conversion.context.ConversionContext;
import ru.kbakaras.e2.conversion.context.ElementProducer;

public class ConversionCopy extends Conversion {
    private ElementProducer copyProducer;

    public ConversionCopy(String destinationEntityName) {
        super(destinationEntityName);

        ElementProducer copyProducer = new ElementProducer();
        copyProducer.attributes().copyUntouched();
        copyProducer.tables().copyUntouchedTables();
    }

    @Override
    public void convertElement(ConversionContext context) {
        context.addResult(copyProducer);
    }
}