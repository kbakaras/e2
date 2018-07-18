package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.message.E2EntityRequest;
import ru.kbakaras.e2.message.E2ReferenceRequest;
import ru.kbakaras.e2.message.E2Request;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.Map;

public class Converter4Request {
    public final E2Request input;
    public final E2Request output;

    public final Lazy<Converter4Payload> context;

    private Conversions conversions;

    public Converter4Request(E2Request input, E2Request output, Map<String, Class<? extends Conversion>> conversionMap) {
        this.input = input;
        this.output = output;
        this.conversions = new Conversions(conversionMap);

        context = Lazy.of(() ->
                new Converter4Payload(input.context().get(), output.createContext(), conversions));
    }

    public void convertReferenceRequest(E2ReferenceRequest source) {
        for (String destinationEntity: conversions.get(source.entityName()).destinationEntities) {
            output.addReferenceRequest(destinationEntity, source.elementUid());
        }
    }

    public void convertEntityRequest(E2EntityRequest source) {
        Conversion conversion = conversions.get(source.entityName());

        for (String destinationEntity: conversion.destinationEntities) {
            E2EntityRequest destination = output.addEntityRequest(destinationEntity);
            source.filters().forEach(filter ->
                    conversion.conversionRules4Filters.get().apply(filter, destination, this));
        }
    }
}