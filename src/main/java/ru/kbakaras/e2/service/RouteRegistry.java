package ru.kbakaras.e2.service;

import ru.kbakaras.e2.model.RouteRequest;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.RouteRequestRepository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RouteRegistry {
    @Resource
    private RouteRequestRepository routeRequestRepository;

    public Set<SystemInstance> findRequestRoute(SystemInstance requestor, Collection<SystemInstance> destinations, String entityName) {
        List<RouteRequest> list;
        if (destinations.isEmpty()) {
            list = routeRequestRepository.getByRequestorAndRequestorEntityName(
                    requestor, entityName);
        } else {
            list = routeRequestRepository.getByRequestorAndRequestorEntityNameAndResponderIn(
                    requestor, entityName, destinations);
        }

        return list.stream()
                .map(RouteRequest::getResponder)
                .collect(Collectors.toCollection(HashSet::new));
    }
}