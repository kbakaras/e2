package ru.kbakaras.e2.conversion.context;

public interface IProducers4Attributes extends IProducers {
    IProducers4Attributes copyUntouched();
    IProducers4Attributes skip(String... attributeNames);

    Producers4Attributes.Producer4AttributeSetup attribute(String attributeName);
}