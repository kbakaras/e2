package ru.kbakaras.e2.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.service.ConfigurationManager;

import java.util.Map;
import java.util.UUID;

/**
 * Класс инкапсулирует всю текущую конфигурацию e2. В нём содержатся маршруты,
 * конверсии и экземпляры систем. См. также {@link ru.kbakaras.e2.service.ConfigurationManager}.
 */
public class Configuration4E2 {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);

    private Map<Class<? extends SystemType>, Map<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>>> sources;

    public Configuration4E2(Map<Class<? extends SystemType>, Map<Class<? extends SystemType>, Map<String, Class<? extends Conversion>>>> sources) {
        this.sources = sources;
    }


    public SystemAccessor getSystemAccessor(UUID systemUid) {
        return null;
    }


    @Override
    protected void finalize() throws Throwable {

        LOG.info("Garbage collection of " + this.toString());

        super.finalize();

    }

}