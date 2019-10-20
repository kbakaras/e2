package ru.kbakaras.e2.testing;

import ru.kbakaras.e2.core.conversion.Conversion;
import ru.kbakaras.e2.core.conversion.Conversions;
import ru.kbakaras.e2.core.conversion.PayloadConversionBind;
import ru.kbakaras.sugar.spring.PackageResolver;

import java.util.HashMap;
import java.util.Map;

public class TestConfigurer {

    public static Conversions configureConversions(String basePackage) {

        Map<String, Class<? extends Conversion>> conversionMap = new HashMap<>();

        new PackageResolver().forEach(basePackage, PayloadConversionBind.class, (bindClass, props) -> {

            String sourceEntity = (String) props.get("sourceEntity");
            conversionMap.put(sourceEntity, bindClass);

        });

        return new Conversions(conversionMap);

    }

}