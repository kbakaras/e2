package ru.kbakaras.e2.message;

public abstract class E2AttributeValue {
    abstract public <R extends E2Referring<?>> R apply(R referring);
}