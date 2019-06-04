package ru.kbakaras.e2.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.service.ConfigurationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Класс инкапсулирует всю текущую конфигурацию e2. В нём содержатся маршруты,
 * конверсии и экземпляры систем. См. также {@link ru.kbakaras.e2.service.ConfigurationManager}.
 */
public class Configuration4E2 {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);

    private Source2Destinations4Conversions conversionClasses;
    private RouteMap updateRoutes;
    private RouteMap requestRoutes;


    public Configuration4E2(Source2Destinations4Conversions conversionClasses, RouteMap updateRoutes, RouteMap requestRoutes) {
        this.conversionClasses = conversionClasses;
        this.updateRoutes = updateRoutes;
        this.requestRoutes = requestRoutes;
    }


    public SystemAccessor getSystemAccessor(UUID systemUid) {
        return null;
    }


    @Override
    protected void finalize() throws Throwable {

        LOG.info("Garbage collection of " + this.toString());

        super.finalize();

    }


    public static class Destination2Conversions extends HashMap<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>> {}
    public static class Source2Destinations4Conversions extends HashMap<Class<? extends SystemType>, Destination2Conversions> {}

    public static class RouteMap extends HashMap<UUID, Map<String, UUID>> {};

}