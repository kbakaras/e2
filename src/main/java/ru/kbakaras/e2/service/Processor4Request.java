package ru.kbakaras.e2.service;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.core.conversion.Converter4Payload;
import ru.kbakaras.e2.core.conversion.Converter4Request;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.message.E2;
import ru.kbakaras.e2.message.E2Request;
import ru.kbakaras.e2.message.E2Response;
import ru.kbakaras.e2.message.E2SystemResponse;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.sugar.lazy.MapCache;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class Processor4Request {
    private static final Logger LOG = LoggerFactory.getLogger(Processor4Request.class);

    @Resource private ConfigurationManager configurationManager;


    public Element elementRequest(Element request) {
        return process(request);
    }

    public Element listRequest(Element request) {
        return process(request);
    }


    private Element process(Element request) {

        Configuration4E2 conf = configurationManager.getConfiguration();

        E2Request sourceRequest = new E2Request(request);

        UUID   sourceId   = sourceRequest.sourceSystemUid();
        String sourceName = conf.getSystemInstance(sourceId).getName();


        /*
         * Кэш накапливает наборы идентификаторов систем, в которые должен быть направлен запрос,
         * полученный из исходной сущности. Учитывается ограничивающий набор сущностей, если таковой указан
         * в сообщении запроса (то есть результатом будет конъюнкция этого набора и набора, полученного
         * из маршрутизации).
         */
        MapCache<String, Set<UUID>> destinations4entity = MapCache.of(
                entityName -> conf.getRequestDestinations(
                        sourceId, entityName,
                        sourceRequest.destinationSystemUids()
                )
        );

        /*
          Кэш накапливает результаты конвертации запросов для каждой
          запрашиваемой системы, по мере необходимости.
         */
        MapCache<UUID, Converter4Request> converters4request = MapCache.of(
                destinationId -> new Converter4Request(
                        sourceRequest,
                        new E2Request(sourceRequest.requestType())
                                .setSourceSystem(sourceId.toString(), sourceName)
                                .addDestinationSystem(destinationId.toString()),
                        conf.getConversions(sourceId, destinationId)
                )
        );

        /*
          Перебор всех запросов за элементами по UID. Для каждого запроса известна сущность, к которй идёт запрос.
          Для сущности определяется список запрашиваемых систем в соответствии с имеющимися маршрутами для запросов,
          а также учитываются ограничения, переданные в сообщении с запросом.
         */
        sourceRequest.references().forEach(referenceRequest ->
            destinations4entity.get(referenceRequest.entityName()).forEach(
                    destinationId -> converters4request.get(destinationId)
                            .convertReferenceRequest(referenceRequest))
        );

        /*
          Перебор всех запросов за элементами, удовлетворяющими фильтрам. Аналогично предыдущему объекту.
         */
        sourceRequest.entities().forEach(entityRequest ->
            destinations4entity.get(entityRequest.entityName()).forEach(
                    destinationId -> converters4request.get(destinationId)
                            .convertEntityRequest(entityRequest))
        );


        LOG.info("Запросы от {} будут направлены в системы:", sourceName);
        List<Callable<ResponseContainer>> tasks = new ArrayList<>();

        converters4request.getMap().forEach((destinationId, converter) -> {

            SystemConnection destination = conf.getSystemConnection(destinationId);

            Element donorRequest = destination.convertRequest(converter.output.xml());
            LOG.info("    {}", destination.systemName);

            tasks.add(() -> new ResponseContainer(destination, destination.request(donorRequest)));

        });


        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, tasks.size()));

        try {
            List<Future<ResponseContainer>> responseFutures = executorService.invokeAll(tasks);

            E2Response outResponse = new E2Response(sourceRequest.requestType());

            for (Future<ResponseContainer> responseFuture: responseFutures) {
                ResponseContainer responseContainer = responseFuture.get();

                if (!responseContainer.response.getName().equals(E2.ERROR)) {
                    E2SystemResponse inSystemResponse =
                            responseContainer.systemConnection.convertResponse(responseContainer.response).systemResponse();

                    if (inSystemResponse != null) {
                        new Converter4Payload(
                                inSystemResponse,
                                outResponse.addSystemResponse(
                                        responseContainer.systemConnection.getId().toString(),
                                        responseContainer.systemConnection.getName()),
                                conf.getConversions(sourceId, responseContainer.systemConnection.systemId)
                        ).convertChanged();
                    }

                } else {
                    outResponse.addSystemError(
                            responseContainer.systemConnection.getId().toString(),
                            responseContainer.systemConnection.getName(),
                            responseContainer.response);
                }
            }

            return outResponse.xml();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);

        } finally {
            executorService.shutdown();
        }

    }


    private static class ResponseContainer {

        final SystemConnection systemConnection;
        final Element response;

        private ResponseContainer(SystemConnection systemConnection, Element response) {
            this.systemConnection = systemConnection;
            this.response = response;
        }

    }
}