package ru.kbakaras.e2.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.RouteRequest;
import ru.kbakaras.e2.model.SystemAccessor;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.sugar.tree.MappedTree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RouteRegistry implements InitializingBean {
    //@Resource
    private RouteRequestRepository routeRequestRepository;

    private MappedTree routes4Update;
    private MappedTree routes4Request;


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
    @SuppressWarnings("unchecked")
    public Set<SystemAccessor> getRequestDestinations(
            SystemAccessor requestorAccessor,
            Collection<SystemAccessor> destinations,
            String requestorEntityName) {

        Set<SystemAccessor> result = getDestinations(routes4Request, requestorAccessor, requestorEntityName);

        if (destinations != null || !destinations.isEmpty()) {
            result.retainAll(destinations);
        }

        return result;
    }


    public Set<SystemAccessor> getUpdateDestinations(
            SystemAccessor sourceAccessor,
            String sourceEntityName) {

        return getDestinations(routes4Update, sourceAccessor, sourceEntityName);
    }

    public boolean isExistUpdateRoute(
            SystemAccessor sourceAccessor,
            SystemAccessor destinationAccessor,
            String sourceEntityName) {

        return getUpdateDestinations(sourceAccessor, sourceEntityName)
                .contains(destinationAccessor);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        routes4Update  = new MappedTree();
        routes4Request = new MappedTree();
    }


    @SuppressWarnings("unchecked")
    private static Set<SystemAccessor> getDestinations(MappedTree routes, SystemAccessor accessor, String entity) {
        Set<SystemAccessor> result = (Set<SystemAccessor>) routes.getValue(accessor, entity);
        return result != null ? result : EMPTY_SET;
    }

    private static final Set<SystemAccessor> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>(0));
}