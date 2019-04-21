package ru.kbakaras.e2.service;

import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.RouteRequest;
import ru.kbakaras.e2.model.SystemAccessor;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.RouteRequestRepository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
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

    /**
     * @param requestorAccessor   Запрашивающая система
     * @param destinations        Список запрашиваемых систем. Если в данном параметре указан не пустой список,
     *                            он будет использоваться в качестве фильтра: в результирующий набор попадут только
     *                            те системы, для которых найдётся маршрут, и только если они перечислены в списке.
     * @param requestorEntityName Имя сущности (для запрашивающей системы), для которой выполняется поиск маршрута.
     * @return Множество систем, до которых имеются request-маршруты для указанной сущности.
     */
    public Set<SystemAccessor> getRequestDestinations(SystemAccessor requestorAccessor, Collection<SystemAccessor> destinations, String requestorEntityName) {
        if (destinations == null || destinations.isEmpty()) {
            return null;
        } else {
            return null;
        }
    }


    public Set<SystemAccessor> getUpdateDestinations(SystemAccessor sourceAccessor, String sourceEntityName) {
        return null;
    }

    public boolean isExistUpdateRoute(SystemAccessor sourceAccessor, SystemAccessor destinationAccessor, String sourceEntityName) {
        return false;
    }
}