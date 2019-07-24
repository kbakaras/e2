package ru.kbakaras.e2.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.kbakaras.e2.core.RouteConfigurer;
import ru.kbakaras.e2.core.conversion.Conversion;
import ru.kbakaras.e2.core.conversion.PayloadConversionBind;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.core.model.SystemType;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Configuration4E2.RouteMap;
import ru.kbakaras.e2.model.Configuration4E2.Source2Destinations4Conversions;
import ru.kbakaras.e2.model.Configuration4E2Exception;
import ru.kbakaras.e2.model.ConfigurationData;
import ru.kbakaras.e2.model.ConfigurationInfo;
import ru.kbakaras.e2.model.ConfigurationReference;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repository.ConfigurationInfoRepository;
import ru.kbakaras.e2.repository.ConfigurationReferenceRepository;
import ru.kbakaras.e2.repository.SystemInstanceRepository;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.jpa.ProperEntity;
import ru.kbakaras.sugar.lazy.MapCache;
import ru.kbakaras.sugar.spring.PackageResolver;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
@Slf4j
@Service
public class ConfigurationManager implements InitializingBean {

    volatile private Configuration4E2 configuration;

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private SystemInstanceRepository systemRepository;

    @Resource
    private ConfigurationInfoRepository configurationInfoRepository;

    @Resource
    private ConfigurationReferenceRepository configurationReferenceRepository;


    @Override
    public void afterPropertiesSet() {

        updateConfiguration();
    }


    private synchronized void updateConfiguration() {

        ConfigurationReference configurationReference = configurationReferenceRepository.findFirstByOrderByCreatedDesc();

        if (configurationReference != null) {

            log.info("Loading configuration {}", configurationReference);

            updateConfiguration(
                    configurationReference,
                    entityManager.find(ConfigurationData.class, configurationReference.getInfo().getId()).getData()
                    );

        } else {

            log.warn("No configurations found in DB! Empty stub will be used.");

            this.configuration = new Configuration4E2(
                    new Source2Destinations4Conversions(),
                    new RouteMap(),
                    new RouteMap(),
                    new HashMap<>(),
                    new HashMap<>(),
                    null,
                    null
            );

        }

    }

    /**
     * Выполняет загрузку в базу и применяет к текущему процессу новую конфигурацию, если она отличается
     * от текущей используемой.
     *
     * @return null, если новая конфигурация применена. Если же новая конфигурация оказалась эквивалентна
     * текущей, возвращается текущая конфигурация (конкретно, объект класса {@link ConfigurationReference}).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized ConfigurationReference updateConfiguration(byte[] data, String fileName) {

        String sha = DigestUtils.sha1Hex(data);
        int size = data.length;

        ConfigurationInfo configurationInfo = configurationInfoRepository.findByShaAndSize(sha, size).stream()
                .filter(info -> Arrays.equals(entityManager.find(ConfigurationData.class, info.getId()).getData(), data))
                .findFirst()
                .orElseGet(() -> {

                    ConfigurationInfo newConfigurationInfo = BaseEntity.newElement(ConfigurationInfo.class);
                    newConfigurationInfo.setSha(sha);
                    newConfigurationInfo.setSize(size);

                    ConfigurationData newConfigurationData = new ConfigurationData();
                    newConfigurationData.setId(newConfigurationInfo.getId());
                    newConfigurationData.setData(data);

                    entityManager.persist(newConfigurationInfo);
                    entityManager.persist(newConfigurationData);

                    return newConfigurationInfo;

                });


        if (configuration.configurationReference == null || !configuration.configurationReference.getInfo().equals(configurationInfo)) {
            ConfigurationReference configurationReference = BaseEntity.newElement(ConfigurationReference.class);
            configurationReference.setInfo(configurationInfo);
            configurationReference.setFileName(fileName);
            entityManager.persist(configurationReference);

            updateConfiguration(configurationReference, data);

            return configurationReference;

        }

        return null;

    }

    /**
     * Выполняет загрузку указанного массива байт в качестве новой конфигурации.
     */
    @SuppressWarnings("unchecked")
    private void updateConfiguration(ConfigurationReference configurationReference, byte[] data) {

        try {

            File jarPath = File.createTempFile("e2_", ".jar");
            jarPath.deleteOnExit();
            FileUtils.writeByteArrayToFile(jarPath, data);

            log.info("Configuration written to file {}", jarPath);

            URLClassLoader configurationClassLoader = new URLClassLoader(new URL[]{jarPath.toURI().toURL()});

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

            Map<UUID, SystemInstance>   instances   = new HashMap<>();
            Map<UUID, SystemConnection> connections = new HashMap<>();

            clazz.newInstance().setupRoutes(
                    (from, to, entities) -> configureRoutes(updateRoutes,  instances, connections, from, to, entities),
                    (from, to, entities) -> configureRoutes(requestRoutes, instances, connections, from, to, entities)
            );


            this.configuration = new Configuration4E2(
                    configureConversions(configurationClassLoader, basePackage),
                    updateRoutes, requestRoutes,
                    instances, connections,
                    configurationReference,
                    jarPath
            );

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
            throw new RuntimeException(e);
        }

    }


    private void registerSystem(Map<UUID, SystemInstance> instances, Map<UUID, SystemConnection> connections,
                                SystemConnection systemConnection) {

        SystemConnection connection = connections.get(systemConnection.getId());
        if (connection == null) {
            connections.put(systemConnection.getId(), systemConnection);

        } else if (connection != systemConnection) {
            throw new Configuration4E2Exception(MessageFormat.format(
                    "Other connection with same id ({1}) already registered!", systemConnection.getId()
            ));
        }


        SystemInstance instance = instances.get(systemConnection.getId());

        if (instance == null) {
            instance = systemRepository.findById(systemConnection.getId())
                    .orElseGet(() -> {
                        SystemInstance newInstance = ProperEntity.newElement(SystemInstance.class);
                        newInstance.setName(systemConnection.systemName);
                        newInstance.setId(systemConnection.getId());
                        return systemRepository.save(newInstance);
                    });
            instances.put(instance.getId(), instance);
        }

        if (!Objects.equals(instance.getName(), systemConnection.getName())) {
            instance.setName(systemConnection.getName());
            instance = systemRepository.save(instance);
            instances.put(instance.getId(), instance);
        }

    }

    private void configureRoutes(RouteMap routeMap,
                                 Map<UUID, SystemInstance> instances, Map<UUID, SystemConnection> connections,
                                 SystemConnection from, SystemConnection to, String...entities) {

        registerSystem(instances, connections, from);
        registerSystem(instances, connections, to);


        Map<String, Set<UUID>> map = routeMap.get(from.systemId);
        if (map == null) {
            routeMap.put(from.getId(), map = new HashMap<>());
        }

        for (String entity: entities) {

            Set<UUID> set = map.computeIfAbsent(entity, k -> new HashSet<>());
            set.add(to.systemId);

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

            String sourceEntity = (String) props.get("sourceEntity");

            mc.get(sourceType).get(destinationType).put(sourceEntity, bindClass);

            log.info("Конверсия {} --> {} :: [{}]", sourceType.getSimpleName(), destinationType.getSimpleName(), sourceEntity);

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