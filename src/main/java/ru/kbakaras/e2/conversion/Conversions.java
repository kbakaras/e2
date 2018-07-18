package ru.kbakaras.e2.conversion;

import ru.kbakaras.sugar.lazy.MapCache;

import java.util.Map;

public class Conversions extends MapCache<String, Conversion> {
    private Map<String, Class<? extends Conversion>> conversionMap;

    public Conversions(Map<String, Class<? extends Conversion>> conversionMap) {
        super(inputName -> {
            Class<? extends Conversion> clazz = conversionMap.get(inputName);
            if (clazz != null) {
                try {
                    return clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return new ConversionCopy(inputName);
            }
        });
        this.conversionMap = conversionMap;
    }
}