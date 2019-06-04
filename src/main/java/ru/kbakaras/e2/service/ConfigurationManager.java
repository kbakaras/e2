package ru.kbakaras.e2.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.core.RouteConfigurer;
import ru.kbakaras.e2.core.conversion.PayloadConversionBind;
import ru.kbakaras.e2.core.model.SystemInstanceBase;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Configuration4E2.RouteMap;
import ru.kbakaras.e2.model.Configuration4E2.Source2Destinations4Conversions;
import ru.kbakaras.e2.model.SystemType;
import ru.kbakaras.sugar.lazy.MapCache;
import ru.kbakaras.sugar.spring.PackageResolver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarFile;

/**
 * Класс предоставляет клиентам объект, содержащий текущую актуальную конфигурацию e2.
 * Конфигурация содержит маршрутизацию для обновлений и запросов, реестр конверсий,
 * реестр систем.<br/><br/>
 * <p>
 * Класс нужен для того, чтобы отвязать жизненный цикл конфигурации от клиентского кода.
 * Это позволяет обновлять конфигурацию "на лету" без перезапуска сервера e2.
 */
@Service
public class ConfigurationManager implements InitializingBean {

    volatile private Configuration4E2 configuration;


    @Override
    public void afterPropertiesSet() throws Exception {
        updateConfiguration();
    }

    @SuppressWarnings("unchecked")
    public void updateConfiguration() {
        try {
            String jarPath = "/home/kbakaras/projects/idea/glance/e2-common/build/libs/e2-common-1.0-SNAPSHOT.jar";

            URLClassLoader configurationClassLoader = new URLClassLoader(new URL[]{new URL("file://" + jarPath)});

            String basePackage;
            Class<? extends RouteConfigurer> clazz;

            try (JarFile jf = new JarFile(jarPath)) {
                String mainClass = jf.getManifest().getMainAttributes().getValue(RouteConfigurer.CONFIGURER_CLASS);

                clazz = (Class<? extends RouteConfigurer>) configurationClassLoader.loadClass(mainClass);
                basePackage = jf.getManifest().getMainAttributes().getValue(RouteConfigurer.CONVERSION_PACKAGE);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            RouteMap updateRoutes  = new RouteMap();
            RouteMap requestRoutes = new RouteMap();


            clazz.newInstance().setupRoutes(
                    (from, to, entities) -> configureRoutes(updateRoutes,  from, to, entities),
                    (from, to, entities) -> configureRoutes(requestRoutes, from, to, entities)
            );


            this.configuration = new Configuration4E2(
                    configureConversions(configurationClassLoader, basePackage),
                    updateRoutes, requestRoutes
            );

        } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureRoutes(RouteMap routeMap, SystemInstanceBase from, SystemInstanceBase to, String...entities) {

        Map<String, UUID> map = routeMap.get(from.systemId);
        if (map == null) {
            routeMap.put(from.getId(), map = new HashMap<>());
        }

        for (String entity: entities) {
            map.put(entity, to.systemId);
        }

    }

    @SuppressWarnings("unchecked")
    private Source2Destinations4Conversions configureConversions(ClassLoader classLoader, String basePackage) {

        MapCache<Class<? extends SystemType>, MapCache<Class<? extends SystemType>,
                Map<String, Class<? extends Conversion>>>> mc = MapCache.of(
                source -> MapCache.of(destination -> new HashMap<>())
        );

        PackageResolver resolver = new PackageResolver(classLoader);
        resolver.forEach(basePackage, PayloadConversionBind.class, (bindClass, props) -> {

            @SuppressWarnings("unchecked")
            Class<? extends SystemType> sourceType = (Class<? extends SystemType>) props.get("sourceType");

            @SuppressWarnings("unchecked")
            Class<? extends SystemType> destinationType = (Class<? extends SystemType>) props.get("destinationType");

            mc.get(sourceType).get(destinationType)
                    .put((String) props.get("sourceEntity"), bindClass);

        });

        Source2Destinations4Conversions source2Destinations = new Source2Destinations4Conversions();

        for (Map.Entry<Class<? extends SystemType>, MapCache<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>>> entrySource: mc) {

            Configuration4E2.Destination2Conversions destination2Conversions = new Configuration4E2.Destination2Conversions();
            source2Destinations.put(entrySource.getKey(), destination2Conversions);

            for (Map.Entry<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>> entryDestination: entrySource.getValue()) {

                Map<String, Class<? extends Conversion>> conversions =
                        Collections.unmodifiableMap(entryDestination.getValue());
                destination2Conversions.put(entryDestination.getKey(), conversions);

            }
        }

        return source2Destinations;

    }



    public Configuration4E2 getConfiguration() {
        return configuration;
    }

}