package ru.kbakaras.e2.converted;

import ru.kbakaras.e2.conversion.ConversionKind;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;

public class ConvertedSimple extends Converted {
    private E2Reference value;

    ConvertedSimple(ConversionKind kind) {
        super(kind);
    }

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
            throw new E2Exception4Write("Requested entity is not the same as of converted value!");
        }
    }
}