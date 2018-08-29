package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.sugar.lazy.MapCache;

import java.util.HashMap;
import java.util.Map;

public class ConvertedCache {
    private MapCache<String, Map<String, Converted>> converted = MapCache.of(entityName -> new HashMap<>());

    public void init(E2Element element, ConversionKind kind) {
        converted.get(element.entityName()).put(
                element.getUid(), Converted.create(element, kind));
    }

    public boolean isConverting(E2Element element) {
        return converted.get(element.entityName()).containsKey(element.getUid());
    }

    public Converted get(E2Element element) {
        return converted.get(element.entityName()).get(element.getUid());
    }
}