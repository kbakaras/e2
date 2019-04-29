package ru.kbakaras.e2.service;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.RouteRequest;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.jpa.repository.RegsetRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RouteRequestRepository extends
        JpaRepository<RouteRequest, UUID>,
        RegsetRepository<RouteRequest> {

    List<RouteRequest> getByRequestorAndResponder(SystemInstance requestor, SystemInstance responder);

    List<RouteRequest> getByRequestorAndRequestorEntityNameAndResponderIn(SystemInstance requestor, String requestorEntityName, Collection<SystemInstance> responders);
    List<RouteRequest> getByRequestorAndRequestorEntityName(SystemInstance requestor, String requestorEntityName);
}