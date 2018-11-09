package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.RouteUpdate;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.jpa.repository.RegsetRepository;

import java.util.List;
import java.util.UUID;

public interface RouteUpdateRepository extends
        JpaRepository<RouteUpdate, UUID>,
        RegsetRepository<RouteUpdate> {

    List<RouteUpdate> getBySourceAndDestination(SystemInstance source, SystemInstance destination);
    List<RouteUpdate> getBySourceAndSourceEntityName(SystemInstance source, String sourceEntityName);

    boolean existsBySourceAndDestinationAndSourceEntityName(SystemInstance source, SystemInstance destination, String sourceEntityName);
}