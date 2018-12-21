package ru.kbakaras.e2.conversion;

public abstract class SynthCache {
    protected Converter4Payload converter;

    final void setConverter(Converter4Payload converter) {
        this.converter = converter;
    }
}