package ru.kbakaras.e2.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Configuration4E2.RouteMap;
import ru.kbakaras.e2.model.Configuration4E2.Source2Destinations4Conversions;

import java.util.HashMap;
import java.util.UUID;

class EmptyConfigurationTest {

    @Test
    void basicTest() {

        Configuration4E2 configuration = new Configuration4E2(
                new Source2Destinations4Conversions(),
                new RouteMap(),
                new RouteMap(),
                new HashMap<>(),
                new HashMap<>(),
                null,
                null
        );


        Assertions.assertNull(configuration.getSystemInstance(UUID.randomUUID()));
        Assertions.assertNull(configuration.getSystemConnection(UUID.randomUUID()));

        Assertions.assertNull(configuration.getConversions(UUID.randomUUID(), UUID.randomUUID()));

        Assertions.assertTrue(
                configuration.getUpdateDestinations(UUID.randomUUID(), "Справочник.ИмяСправочника")
                        .isEmpty()
        );

        Assertions.assertTrue(
                configuration.getRequestDestinations(UUID.randomUUID(), "Справочник.ИмяСправочника", new UUID[0])
                        .isEmpty()
        );

        Assertions.assertFalse(
                configuration.updateRouteExists(UUID.randomUUID(), UUID.randomUUID(), "Справочник.ИмяСправочника")
        );

    }

}