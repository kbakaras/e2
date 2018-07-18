package ru.kbakaras.e2.converted;

import ru.kbakaras.e2.conversion.ConversionKind;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Reference;

/**
 * В отличие от ConvertedMerge и ConvertedSimple, если запросить у этого
 * объекта значение для явно указанной сущности, которая не совпадёт с
 * сущностью хранящегося в нём значения, исключение выброшено не будет,
 * метод просто вернёт null.
 */
public class ConvertedChoice extends Converted {
    private E2Reference value;

    ConvertedChoice(ConversionKind kind) {
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
    protected E2AttributeValue getValue(String explicitEntity) {
        if (explicitEntity == null || value.entityName.equals(explicitEntity)) {
            return value;
        } else {
            return null;
        }
    }
}