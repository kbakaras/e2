package ru.kbakaras.e2.message;

import org.butu.sugar.lazy.Lazy;

public abstract class E2AttributeValue {
    abstract public <R extends E2Referring<?>> R apply(R referring);

    public static final Lazy<E2AttributeValue> empty = Lazy.of(
            () -> new E2AttributeValue() {
                @Override
                public <R extends E2Referring<?>> R apply(R referring) {
                    throw new E2Exception4Write("Empty content could not be applied!");
                }
            });
}
