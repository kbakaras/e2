package ru.kbakaras.e2.service;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.conversion.Converter4Request;
import ru.kbakaras.e2.message.E2;
import ru.kbakaras.e2.message.E2Request;
import ru.kbakaras.e2.message.E2Response;
import ru.kbakaras.e2.message.E2SystemResponse;
import ru.kbakaras.e2.model.SystemAccessor;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.sugar.lazy.MapCache;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class Processor4Request {
    private static final Logger LOG = LoggerFactory.getLogger(Processor4Request.class);


    @Resource private ConversionRegistry conversionRegistry;
    @Resource private RouteRegistry      routeRegistry;
    @Resource private AccessorRegistry   accessorRegistry;


    public Element elementRequest(Element request) {
        return process(request);
    }

    public Element listRequest(Element request) {
        return process(request);
    }


    private Element process(Element request) {
        E2Request sourceRequest = new E2Request(request);

        SystemAccessor sourceAccessor = accessorRegistry.get(sourceRequest.sourceSystemUid());
        //SystemInstance sourceSystem = systemInstanceRepository.findById(sourceRequest.sourceSystemUid()).get();
        //updateSystemName(sourceSystem, sourceRequest.sourceSystemName());

        //List<SystemInstance> enabledDestinations = systemInstanceRepository.findAllById(Arrays.asList(sourceRequest.destinationSystemUids()));
        Set<SystemAccessor> enabledDestinations = accessorRegistry.get(sourceRequest.destinationSystemUids());

        MapCache<String, Set<SystemAccessor>> destinations4entity = MapCache.of(
                entityName -> routeRegistry.getRequestDestinations(sourceAccessor, enabledDestinations, entityName));

        /*
          Кэш накапливает результаты конвертации запросов для каждой
          запрашивающей системы, по мере необходимости.
         */
        MapCache<SystemAccessor, Converter4Request> converters4request = MapCache.of(
                destinationAccessor -> new Converter4Request(
                        sourceRequest,
                        new E2Request(sourceRequest.requestType())
                                .setSourceSystem(sourceAccessor.getId().toString(), sourceAccessor.getName())
                                .addDestinationSystem(destinationAccessor.getId().toString()),
                        conversionRegistry.get(sourceAccessor, destinationAccessor))
        );

        sourceRequest.references().forEach(referenceRequest ->
            destinations4entity.get(referenceRequest.entityName()).forEach(
                    destinationAccessor -> converters4request.get(destinationAccessor)
                            .convertReferenceRequest(referenceRequest))
        );

        sourceRequest.entities().forEach(entityRequest ->
            destinations4entity.get(entityRequest.entityName()).forEach(
                    destinationAccessor -> converters4request.get(destinationAccessor)
                            .convertEntityRequest(entityRequest))
        );

        LOG.info("Запросы от {} будут направлены в системы:", sourceAccessor);
        List<Callable<DonorResponse>> tasks = new ArrayList<>();
        converters4request.getMap().forEach((destinationAccessor, converter) -> {
            Element donorRequest = destinationAccessor.convertRequest(converter.output.xml());
            LOG.info("    {}", destinationAccessor);

            tasks.add(() -> new DonorResponse(destinationAccessor, destinationAccessor.request(donorRequest)));
        });

        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, tasks.size()));

        try {
            List<Future<DonorResponse>> donorResponseFutures = executorService.invokeAll(tasks);

            E2Response outResponse = new E2Response(sourceRequest.requestType());

            for (Future<DonorResponse> donorResponseFuture: donorResponseFutures) {
                DonorResponse donorResponse = donorResponseFuture.get();

                if (!donorResponse.response.getName().equals(E2.ERROR)) {
                    E2SystemResponse inSystemResponse =
                            donorResponse.systemAccessor.convertResponse(donorResponse.response).systemResponse();

                    if (inSystemResponse != null) {
                        //updateSystemName(donorResponse.systemAccessor.systemInstance, inSystemResponse.responseSystemName());

                        new Converter4Payload(
                                inSystemResponse,
                                outResponse.addSystemResponse(
                                        donorResponse.systemAccessor.getId().toString(),
                                        donorResponse.systemAccessor.getName()),
                                conversionRegistry.get(donorResponse.systemAccessor, sourceAccessor)
                        ).convertChanged();
                    }

                } else {
                    outResponse.addSystemError(
                            donorResponse.systemAccessor.getId().toString(),
                            donorResponse.systemAccessor.getName(),
                            donorResponse.response);
                }
            }

            return outResponse.xml();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }


    private void updateSystemName(SystemInstance sourceSystem, String systemName) {

    }


    private static class DonorResponse {
        final SystemAccessor systemAccessor;
        final Element response;

        private DonorResponse(SystemAccessor systemAccessor, Element response) {
            this.systemAccessor = systemAccessor;
            this.response = response;
        }
    }
}