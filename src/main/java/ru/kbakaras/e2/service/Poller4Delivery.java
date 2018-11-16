package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.model.History4Delivery;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.Error4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4ConversionRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.e2.repositories.QueueManage;
import ru.kbakaras.e2.repositories.RouteUpdateRepository;
import ru.kbakaras.e2.repositories.SystemInstanceRepository;
import ru.kbakaras.e2.service.rest.ManageQueueException;
import ru.kbakaras.e2.service.rest.ManageQueueSkipException;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class Poller4Delivery extends BasicPoller<Queue4Delivery> {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Delivery.class);

    private boolean stopOnStuck = true;

    @Resource private Queue4DeliveryRepository queue4DeliveryRepository;
    @Resource private Error4DeliveryRepository error4DeliveryRepository;

    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private SystemInstanceRepository systemInstanceRepository;
    @Resource private ConversionRegistry conversionRegistry;
    @Resource private RouteUpdateRepository routeUpdateRepository;
    @Resource private HistoryService historyService;

    synchronized public void resume() {
        if (!isPolling()) {
            queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc()
                    .ifPresent(queue -> {
                        queue.setStuck(false);
                        queue4DeliveryRepository.save(queue);
                    });
            start();
        }
    }

    synchronized public History4Delivery reconvert(Queue4Delivery queue) {
        if (isPolling()) {
            throw new ManageQueueException(
                    "Delivery poller is active! It's not possible to reconvert.");
        }


        Queue4Conversion sourceQueue = queue4ConversionRepository.getOne(queue.getSourceMessageId());

        E2Update sourceMessage = new E2Update(sourceQueue.getMessage());
        SystemInstance source = systemInstanceRepository.getOne(sourceMessage.systemUid());
        SystemInstance destination = queue.getDestination();

        LOG.info("Reconverting message {} from system {} for system {}:",
                sourceQueue.getId(), source, destination);

        E2Update destinationMessage = reconvert(sourceMessage, source, destination);

        if (destinationMessage.entities().isEmpty()) {
            throw new ManageQueueException(String.format(
                    "Message with id (%s) produced result with no entities for destination [%s].",
                    sourceQueue.getId(), destination
            ));
        }

        String newMessage = destinationMessage.xml().asXML();

        if (queue.getMessage().equals(newMessage)) {
            throw new ManageQueueSkipException(String.format(
                    "Original message with id (%s) is equal to reconverted!",
                    sourceQueue.getId()
            ));
        }

        return historyService.reconverted(queue, newMessage);
    }

    private E2Update reconvert(E2Update sourceMessage, SystemInstance source, SystemInstance destination) {
        Converter4Payload converter = new Converter4Payload(sourceMessage,
                new E2Update().setSystemUid(source.getId().toString()).setSystemName(source.getName()),
                conversionRegistry.get(source.getType(), destination.getType())
        );

        Predicate<E2Entity> routeExists = entity ->
            routeUpdateRepository.existsBySourceAndDestinationAndSourceEntityName(
                    source, destination, entity.entityName());

        for (E2Entity entity: sourceMessage.entities()) {
            List<E2Element> elementsChanged = entity.elementsChanged();
            if (!elementsChanged.isEmpty() && routeExists.test(entity)) {
                elementsChanged.forEach(converter::convertElement);
            }
        }

        return (E2Update) converter.output;
    }

    @Override
    protected Optional<Queue4Delivery> next() {
        if (stopOnStuck) {
            return queue4DeliveryRepository.getFirstByProcessedIsFalseOrderByTimestampAsc();
        } else {
            return queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc();
        }
    }

    @Override
    protected QueueManage getQueueManager() {
        return queue4DeliveryRepository;
    }

    @Override
    protected void process(Queue4Delivery queue) {
        try {
            Element update = DocumentHelper.parseText(queue.getMessage()).getRootElement();
            SystemInstance destination = queue.getDestination();

            destination.update(destination.getType().convertRequest(update));
            queue.setDelivered();
            queue.setProcessed(true);

        } catch (Throwable e) {
            queue.incAttempt();
            if (queue.getAttempt() >= ATTEMPT_MAX) {
                queue.setStuck(true);
            }

            Error4Delivery error = BaseEntity.newElement(Error4Delivery.class);
            error.setQueue(queue);
            error.setError(ExceptionUtils.getMessage(e));
            error.setStackTrace(ExceptionUtils.getStackTrace(e));
            error4DeliveryRepository.save(error);

            LOG.error("Update delivery error!{}", error);
        }

        queue4DeliveryRepository.save(queue);
    }

    private static final int ATTEMPT_MAX = 3;
}