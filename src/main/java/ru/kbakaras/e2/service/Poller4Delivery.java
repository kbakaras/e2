package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.core.conversion.Converter4Payload;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.model.History4Delivery;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.repositories.Error4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4ConversionRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.e2.repositories.QueueManage;
import ru.kbakaras.e2.service.rest.ManageQueueException;
import ru.kbakaras.e2.service.rest.ManageQueueSkipException;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class Poller4Delivery extends BasicPoller<Queue4Delivery> {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Delivery.class);

    private boolean stopOnStuck = true;

    @Resource private Queue4DeliveryRepository   queue4DeliveryRepository;
    @Resource private Error4DeliveryRepository   error4DeliveryRepository;
    @Resource private Queue4ConversionRepository queue4ConversionRepository;

    @Resource private ConfigurationManager configurationManager;

    @Resource private HistoryService     historyService;


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

        Configuration4E2 conf = configurationManager.getConfiguration();

        Queue4Conversion sourceQueue = queue4ConversionRepository.getOne(queue.getSourceMessageId());

        E2Update sourceMessage = new E2Update(sourceQueue.getMessage());

        UUID        sourceId   = sourceMessage.systemUid();
        String      sourceName = conf.getSystemInstance(sourceId).getName();
        UUID   destinationId   = queue.getDestination().getId();
        String destinationName = conf.getSystemInstance(destinationId).getName();

        LOG.info("Reconverting message {} from system {} for system {}:",
                sourceQueue.getId(), sourceName, destinationName);


        Converter4Payload converter = new Converter4Payload(sourceMessage,
                new E2Update()
                        .setSystemUid(sourceId.toString())
                        .setSystemName(sourceName),
                conf.getConversions(sourceId, destinationId)
        );


        Predicate<E2Entity> routeExists = entity ->
                conf.updateRouteExists(sourceId, destinationId, entity.entityName());

        for (E2Entity entity: sourceMessage.entities()) {
            List<E2Element> elementsChanged = entity.elementsChanged();
            if (!elementsChanged.isEmpty() && routeExists.test(entity)) {
                elementsChanged.forEach(converter::convertElement);
            }
        }


        E2Update destinationMessage = (E2Update) converter.output;

        if (destinationMessage.entities().isEmpty()) {
            throw new ManageQueueException(String.format(
                    "Message with id (%s) produced result with no entities for destination [%s].",
                    sourceQueue.getId(), destinationName
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

        Configuration4E2 conf = configurationManager.getConfiguration();

        try {
            Element update = DocumentHelper.parseText(queue.getMessage()).getRootElement();
            SystemConnection connection = conf.getSystemConnection(queue.getDestination().getId());

            connection.update(connection.convertRequest(update));
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