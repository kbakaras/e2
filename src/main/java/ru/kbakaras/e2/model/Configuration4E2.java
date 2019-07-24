package ru.kbakaras.e2.model;

import lombok.extern.slf4j.Slf4j;
import ru.kbakaras.e2.core.conversion.Conversion;
import ru.kbakaras.e2.core.conversion.Conversions;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.core.model.SystemType;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Класс инкапсулирует всю текущую конфигурацию e2. В нём содержатся маршруты,
 * конверсии и экземпляры систем. См. также {@link ru.kbakaras.e2.service.ConfigurationManager}.
 */
@Slf4j
public class Configuration4E2 {

    public final ConfigurationReference configurationReference;
    private final File jarFile;

    private Source2Destinations4Conversions conversionClasses;
    private RouteMap updateRoutes;
    private RouteMap requestRoutes;

    private Map<UUID, SystemInstance> instances;
    private Map<UUID, SystemConnection> connections;


    public Configuration4E2(Source2Destinations4Conversions conversionClasses,
                            RouteMap updateRoutes, RouteMap requestRoutes,
                            Map<UUID, SystemInstance> instances,
                            Map<UUID, SystemConnection> connections,
                            ConfigurationReference configurationReference,
                            File jarFile) {

        this.conversionClasses = conversionClasses;
        this.updateRoutes      = updateRoutes;
        this.requestRoutes     = requestRoutes;
        this.instances         = instances;
        this.connections       = connections;

        this.configurationReference = configurationReference;
        this.jarFile = jarFile;

    }


    public SystemInstance getSystemInstance(UUID systemUid) {

        return instances.get(systemUid);

    }

    public SystemConnection getSystemConnection(UUID systemUid) {

        return connections.get(systemUid);

    }


    public Conversions getConversions(UUID sourceId, UUID destinationId) {

        SystemConnection sourceConnection = getSystemConnection(sourceId);
        SystemConnection destinationConnection = getSystemConnection(destinationId);

        if (sourceConnection != null && destinationConnection != null) {

            return new Conversions(
                    conversionClasses
                            .get(sourceConnection.systemType)
                            .get(destinationConnection.systemType)
            );

        }

        return null;

    }


    public Set<UUID> getUpdateDestinations(UUID sourceId, String entityName) {

        return Optional.ofNullable(updateRoutes.get(sourceId))
                .map(map -> map.get(entityName))
                .orElseGet(HashSet::new);

    }

    public boolean updateRouteExists(UUID sourceId, UUID destinationId, String entityName) {

        return Optional.ofNullable(updateRoutes.get(sourceId))
                .map(map -> map.get(entityName))
                .filter(set -> set.contains(destinationId))
                .isPresent();

    }

    public Set<UUID> getRequestDestinations(UUID sourceId, String entityName, UUID[] destinationSystemUids) {

        HashSet<UUID> result = Optional.ofNullable(requestRoutes.get(sourceId))
                .map(map -> map.get(entityName))
                .map(HashSet::new)
                .orElseGet(HashSet::new);

        if (destinationSystemUids.length > 0) {
            result.retainAll(Arrays.asList(destinationSystemUids));
        }

        return result;

    }


    @Override
    protected void finalize() throws Throwable {

        if (configurationReference != null) {
            log.info("Finalizing configuration {}", configurationReference);
        }

        if (jarFile != null && jarFile.exists()) {
            if (jarFile.delete()) {
                log.info("Deleted configuration file {}", jarFile);
            }
        }

        super.finalize();

    }


    public static class Destination2Conversions extends HashMap<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>> {}
    public static class Source2Destinations4Conversions extends HashMap<Class<? extends SystemType>, Destination2Conversions> {}

    public static class RouteMap extends HashMap<UUID, Map<String, Set<UUID>>> {}

}