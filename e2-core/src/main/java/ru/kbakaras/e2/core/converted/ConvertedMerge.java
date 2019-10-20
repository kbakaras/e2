package ru.kbakaras.e2.core.converted;

import ru.kbakaras.e2.core.conversion.ConversionKind;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;

public class ConvertedMerge extends Converted {
    private E2Reference value;

    ConvertedMerge(E2Element source, ConversionKind kind) {
        super(source, kind);
    }

    @Override
    public void put(E2Reference reference) {
        if (isVirgin()) {
            this.value = reference;
        } else {
            warnPutIgnored();
        }
    }

    @Override
    public E2AttributeValue getValue(String explicitEntity) {
        if (explicitEntity == null || value.entityName.equals(explicitEntity)) {
            return value;
        } else {
            throw new E2Exception4Write("Requested entity is not the same as of ru.kbakaras.e2.converted value!");
        }
    }
}