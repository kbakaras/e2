package ru.kbakaras.e2.core.conversion;

import ru.kbakaras.e2.core.conversion.context.ConversionContext;

public abstract class ConversionIgnore extends Conversion {
    public ConversionIgnore() {
        super("IGNORE");
    }

    @Override
    public void convertElement(ConversionContext context) {}
}
