package ru.kbakaras.e2.service.rest;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.conversion.Converter4Request;
import ru.kbakaras.e2.message.E2;
import ru.kbakaras.e2.message.E2Request;
import ru.kbakaras.e2.message.E2Response;
import ru.kbakaras.e2.message.E2SystemResponse;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.Queue4Repeat;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.Queue4ConversionRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4RepeatRepository;
import ru.kbakaras.e2.repositories.SystemInstanceRepository;
import ru.kbakaras.e2.service.ConversionRegistry;
import ru.kbakaras.e2.service.Poller4Conversion;
import ru.kbakaras.e2.service.Poller4Delivery;
import ru.kbakaras.e2.service.RouteRegistry;
import ru.kbakaras.e2.service.TimestampService;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.lazy.MapCache;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Создано: kbakaras, в день: 04.03.2018.
 */
@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_XML_VALUE,
        method = RequestMethod.POST,
        path = "rest")
public class ExchangeRest implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRest.class);

    @Resource private ConversionRegistry conversionRegistry;
    @Resource private RouteRegistry      routeRegistry;
    @Resource private TimestampService   timestampService;
    @Resource private Poller4Conversion  poller4Conversion;
    @Resource private Poller4Delivery    poller4Delivery;

    @Resource private SystemInstanceRepository   systemInstanceRepository;
    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private Queue4DeliveryRepository   queue4DeliveryRepository;
    @Resource private Queue4RepeatRepository     queue4RepeatRepository;

    private void update(Element request) {
        Queue4Conversion queue = BaseEntity.newElement(Queue4Conversion.class);
        queue.setTimestamp(timestampService.get());
        queue.setMessage(request.asXML());
        queue.setSize(queue.getMessage().length());
        queue4ConversionRepository.save(queue);
        poller4Conversion.processPoll();
    }

    @RequestMapping(path = "mock")
    public void mock(@RequestBody Element request) {
        if (request.getName().equals("updateRequest")) {
            update(request);
        }
    }

    @RequestMapping(path = "agr")
    public @ResponseBody Element agr(@RequestBody Element request) {
        if (request.getName().equals("updateRequest")) {
            update(request);
            return null;
        }

        E2Request sourceRequest = new E2Request(request);

        SystemInstance sourceSystem = systemInstanceRepository.findById(sourceRequest.sourceSystemUid()).get();
        updateSystemName(sourceSystem, sourceRequest.sourceSystemName());

        List<SystemInstance> enabledDestinations = systemInstanceRepository.findAllById(Arrays.asList(sourceRequest.destinationSystemUids()));

        MapCache<String, Set<SystemInstance>> destinations4entity = MapCache.of(
                entityName -> routeRegistry.findRequestRoute(sourceSystem, enabledDestinations, entityName));

        MapCache<SystemInstance, Converter4Request> converters4request = MapCache.of(
                destinationSystem -> new Converter4Request(
                        sourceRequest,
                        new E2Request(sourceRequest.requestType())
                                .setSourceSystem(sourceSystem.getId().toString(), sourceSystem.getName())
                                .addDestinationSystem(destinationSystem.getId().toString()),
                        conversionRegistry.get(sourceSystem.getType(), destinationSystem.getType()))
        );

        sourceRequest.references().forEach(referenceRequest -> {
            destinations4entity.get(referenceRequest.entityName()).forEach(
                    destination -> converters4request.get(destination)
                            .convertReferenceRequest(referenceRequest));
        });

        sourceRequest.entities().forEach(entityRequest -> {
            destinations4entity.get(entityRequest.entityName()).forEach(
                    destination -> converters4request.get(destination)
                            .convertEntityRequest(entityRequest));
        });

        LOG.info("Запросы от {} будут направлены в системы:", sourceSystem);
        List<Callable<DonorResponse>> tasks = new ArrayList<>();
        converters4request.getMap().forEach((destination, converter) -> {
            Element donorRequest = destination.getType().convertRequest(converter.output.xml());
            LOG.info("    {}", destination);

            tasks.add(() -> new DonorResponse(destination, destination.request(donorRequest)));
        });

        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, tasks.size()));

        try {
            List<Future<DonorResponse>> donorResponseFutures = executorService.invokeAll(tasks);

            E2Response outResponse = new E2Response(sourceRequest.requestType());

            for (Future<DonorResponse> donorResponseFuture: donorResponseFutures) {
                DonorResponse donorResponse = donorResponseFuture.get();

                if (!donorResponse.response.getName().equals(E2.ERROR)) {
                    E2SystemResponse inSystemResponse =
                            donorResponse.system.getType().convertResponse(donorResponse.response).systemResponse();

                    updateSystemName(donorResponse.system, inSystemResponse.responseSystemName());

                    new Converter4Payload(
                            inSystemResponse,
                            outResponse.addSystemResponse(
                                    donorResponse.system.getId().toString(),
                                    donorResponse.system.getName()),
                            conversionRegistry.get(donorResponse.system.getType(), sourceSystem.getType())
                    ).convertChanged();

                } else {
                    outResponse.addSystemError(
                            donorResponse.system.getId().toString(),
                            donorResponse.system.getName(),
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

    @RequestMapping(path = "delivery.resume")
    public void startDelivery() {
        poller4Delivery.resume();
    }

    @Transactional
    @RequestMapping(path = "repeat.stuck")
    public void repeatStuck() {
        queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc().ifPresent(
                stuck -> {
                    Queue4Conversion source = queue4ConversionRepository.getOne(stuck.getSourceMessageId());
                    LOG.info("Repeating the message {}", source);

                    Element root;
                    try {
                        root = DocumentHelper.parseText(source.getMessage()).getRootElement();
                        root.setQName(new QName("repeatRequest", root.getQName().getNamespace()));
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }

                    SystemInstance system = systemInstanceRepository.getOne(
                            UUID.fromString(root.attributeValue(E2.SYSTEM_UID)));

                    Queue4Repeat repeat = BaseEntity.newElement(Queue4Repeat.class);
                    repeat.setSourceMessageId(source.getId());
                    repeat.setDestination(system);
                    repeat.setTimestamp(timestampService.get());
                    repeat.setMessage(root.getDocument().asXML());
                    repeat.setSize(repeat.getMessage().length());
                    queue4RepeatRepository.save(repeat);

                    List<Queue4Delivery> list = queue4DeliveryRepository.findBySourceMessageId(source.getId());
                    list.forEach(deliver -> {
                        deliver.setProcessed(true);
                        queue4DeliveryRepository.save(deliver);
                    });
                }
        );
    }

    @Override
    public void afterPropertiesSet() {
        String env = System.getProperty("environment");
        boolean development = env != null && env.equals("development");

        //dbInit.createDb();

//        List<HttpMessageConverter<?>> converters = new ArrayList<>();
//        converters.add(new Element4jHttpMessageConverter());
//        RestTemplateBuilder builder = restTemplateBuilder.messageConverters(converters);
    }

    private static class DonorResponse {
        final SystemInstance system;
        final Element response;

        private DonorResponse(SystemInstance system, Element response) {
            this.system = system;
            this.response = response;
        }
    }
}