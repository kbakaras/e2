package ru.kbakaras.e2.core.converted;

import ru.kbakaras.e2.core.conversion.ConversionKind;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Scalar;

public class ConvertedScalar extends Converted {
    private E2Scalar value;

    ConvertedScalar(E2Element source, ConversionKind kind) {
        super(source, kind);
    }

    @Override
    public void put(E2Scalar scalar) {
        if (isVirgin()) {
            value = scalar;
        } else {
            warnPutIgnored();
        }
    }

    @Override
    protected E2AttributeValue getValue(String explicitEntity) {
        if (explicitEntity == null) {
            return this.value;
        } else {
            throw new UnsupportedOperationException(
                    "Method 'getValue' for explicit destinationEntity is not allowed for scalar conversion!"
            );
        }
    }
}