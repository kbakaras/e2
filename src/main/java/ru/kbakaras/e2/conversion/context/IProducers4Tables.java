package ru.kbakaras.e2.conversion.context;

public interface IProducers4Tables extends IProducers {
    IProducers4Attributes copyUntouchedTables();
    IProducers4Attributes skipTables(String... attributeNames);
}