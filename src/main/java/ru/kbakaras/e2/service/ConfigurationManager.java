package ru.kbakaras.e2.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.core.RouteConfigurer;
import ru.kbakaras.e2.core.conversion.PayloadConversionBind;
import ru.kbakaras.e2.model.Configuration4E2;
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

            URL[] classLoaderUrls = new URL[]{new URL("file://" + jarPath)};

            URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);

            String basePackage;
            Class<? extends RouteConfigurer> clazz;

            try (JarFile jf = new JarFile(jarPath)) {
                String mainClass = jf.getManifest().getMainAttributes().getValue(RouteConfigurer.CONFIGURER_CLASS);

                clazz = (Class<? extends RouteConfigurer>) urlClassLoader.loadClass(mainClass);
                basePackage = jf.getManifest().getMainAttributes().getValue(RouteConfigurer.CONVERSION_PACKAGE);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            MapCache<Class<? extends SystemType>, MapCache<Class<? extends SystemType>,
                    Map<String, Class<? extends Conversion>>>> mc = MapCache.of(
                            source -> MapCache.of(destination -> new HashMap<>())
            );

            PackageResolver resolver = new PackageResolver(urlClassLoader);
            resolver.forEach(basePackage, PayloadConversionBind.class, (bindClass, props) -> {

                @SuppressWarnings("unchecked")
                Class<? extends SystemType> sourceType = (Class<? extends SystemType>) props.get("sourceType");

                @SuppressWarnings("unchecked")
                Class<? extends SystemType> destinationType = (Class<? extends SystemType>) props.get("destinationType");

                mc.get(sourceType).get(destinationType)
                        .put((String) props.get("sourceEntity"), bindClass);

            });

            Map<Class<? extends SystemType>, Map<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>>> sources = new HashMap<>();

            for (Map.Entry<Class<? extends SystemType>, MapCache<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>>> entrySource: mc) {

                Map<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>> destinations = new HashMap<>();
                sources.put(entrySource.getKey(), destinations);
                
                for (Map.Entry<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>> entryDestination: entrySource.getValue()) {

                    Map<String, Class<? extends Conversion>> conversions =
                            Collections.unmodifiableMap(entryDestination.getValue());
                    destinations.put(entryDestination.getKey(), conversions);
                    
                }
            }

            this.configuration = new Configuration4E2(sources);

        } catch (MalformedURLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Configuration4E2 getConfiguration() {
        return configuration;
    }

}