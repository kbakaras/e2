package ru.kbakaras.e2.conversion;

public abstract class ConversionIgnore extends Conversion {
    public ConversionIgnore() {
        super("IGNORE");
    }

    @Override
    public void convertElement(ConversionContext context) {}
}
