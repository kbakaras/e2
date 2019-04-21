package ru.kbakaras.e2.service;

import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.conversion.PayloadConversionBind;
import ru.kbakaras.e2.model.SystemAccessor;
import ru.kbakaras.e2.model.SystemType;
import ru.kbakaras.sugar.lazy.MapCache;
import ru.kbakaras.sugar.spring.PackageResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * При инициализации зачитывает все классы, анотированные как конверсии и раскладывает
 * их в карту. По запросу (исходнаяСистема, целеваяСистема) выдаёт карту конверсий
 * для названий сущностей.
 */
public class ConversionRegistry {
    private MapCache<Class<? extends SystemType>,
            MapCache<Class<? extends SystemType>,
                    Map<String, Class<? extends Conversion>>>> mc = MapCache.of(
                            source -> MapCache.of(destination -> new HashMap<>())
    );

    @SuppressWarnings("unchecked")
    public ConversionRegistry(String basePackage) {
        PackageResolver resolver = new PackageResolver();
        resolver.forEach(
                basePackage,
                PayloadConversionBind.class,
                (bindClass, props) -> {
                    Map<String, Class<? extends Conversion>> map = mc.get((Class<? extends SystemType>) props.get("sourceType"))
                            .get((Class<? extends SystemType>) props.get("destinationType"));
                    map.put((String) props.get("sourceEntity"), bindClass);
                });
    }

    public Map<String, Class<? extends Conversion>> get(SystemType source, SystemType destination) {
        return mc.get(source.getClass()).get(destination.getClass());
    }

    public Map<String, Class<? extends Conversion>> get(Class<? extends SystemType> sourceClass, Class<? extends SystemType> destinationClass) {
        return mc.get(sourceClass).get(destinationClass);
    }

    public Map<String, Class<? extends Conversion>> get(SystemAccessor sourceAccessor, SystemAccessor destinationAccessor) {
        return get(sourceAccessor.systemType, destinationAccessor.systemType);
    }
}