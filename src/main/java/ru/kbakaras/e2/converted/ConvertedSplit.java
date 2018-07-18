package ru.kbakaras.e2.converted;

import ru.kbakaras.e2.conversion.ConversionKind;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;

import java.util.HashMap;
import java.util.Map;

public class ConvertedSplit extends Converted {
    private Map<String, E2AttributeValue> destinationEntity2value = new HashMap<>();

    ConvertedSplit(ConversionKind kind) {
        super(kind);
    }

    public void put(E2Reference reference) {
        if (!destinationEntity2value.containsKey(reference.entityName)) {
            destinationEntity2value.put(reference.entityName, reference);
        } else {
            LOG.warn("Converted-object already holds result for entity {}!" +
                    " All subsequent puts for this entity are ignored.",
                    reference.entityName);
        }
    }

    public E2AttributeValue getValue(String explicitEntity) {
        if (explicitEntity != null) {
            return destinationEntity2value.get(explicitEntity);
        } else {
            throw new E2Exception4Write("You have to specify explicit destination entity to get converted value for Split conversion!");
        }
    }
}