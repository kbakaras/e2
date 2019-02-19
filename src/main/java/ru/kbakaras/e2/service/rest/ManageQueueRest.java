package ru.kbakaras.e2.service.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.kbakaras.e2.manage.QueueStats;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.BasicError;
import ru.kbakaras.e2.model.BasicQueue;
import ru.kbakaras.e2.model.History4Delivery;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.Queue4Repeat;
import ru.kbakaras.e2.repositories.Error4ConversionRepository;
import ru.kbakaras.e2.repositories.Error4DeliveryRepository;
import ru.kbakaras.e2.repositories.Error4RepeatRepository;
import ru.kbakaras.e2.repositories.Queue4ConversionRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4RepeatRepository;
import ru.kbakaras.e2.repositories.QueueManage;
import ru.kbakaras.e2.repositories.SystemInstanceRepository;
import ru.kbakaras.e2.service.BasicPoller;
import ru.kbakaras.e2.service.Poller4Conversion;
import ru.kbakaras.e2.service.Poller4Delivery;
import ru.kbakaras.e2.service.Poller4Repeat;
import ru.kbakaras.e2.service.TimestampService;
import ru.kbakaras.jpa.BaseEntity;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        method = RequestMethod.POST,
        path = "manage/Queue")
public class ManageQueueRest {
    private static final Logger LOG = LoggerFactory.getLogger(ManageQueueRest.class);

    @Resource private Poller4Delivery poller4Delivery;
    @Resource private Poller4Conversion poller4Conversion;
    @Resource private Poller4Repeat poller4Repeat;

    @Resource private Queue4DeliveryRepository   queue4DeliveryRepository;
    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private Queue4RepeatRepository     queue4RepeatRepository;

    @Resource private Error4DeliveryRepository   error4DeliveryRepository;
    @Resource private Error4ConversionRepository error4ConversionRepository;
    @Resource private Error4RepeatRepository     error4RepeatRepository;

    @Resource private SystemInstanceRepository   systemInstanceRepository;
    @Resource private TimestampService           timestampService;

    @Resource private ObjectMapper objectMapper;

    @RequestMapping(path = "stats")
    public ObjectNode stats(String value) {
        return objectMapper.createObjectNode()
                .putPOJO("conversion", getConversionStats())
                .putPOJO("delivery",   getDeliveryStats())
                .putPOJO("repeat",     getRepeatStats());
    }

    public QueueStats getConversionStats() {
        return createStats(queue4ConversionRepository, poller4Conversion);
    }

    public QueueStats getDeliveryStats() {
        return createStats(queue4DeliveryRepository, poller4Delivery);
    }

    public QueueStats getRepeatStats() {
        return createStats(queue4RepeatRepository, poller4Repeat);
    }

    private QueueStats createStats(QueueManage statsRepository, BasicPoller poller) {
        return new ru.kbakaras.e2.manage.QueueStats(
                statsRepository.countByProcessedIsTrue(),
                statsRepository.countByProcessedIsTrueAndDeliveredTimestampIsNotNull(),
                statsRepository.countByProcessedIsFalse(),
                statsRepository.countByProcessedIsFalseAndStuckIsTrue(),
                !poller.isPolling()
        );
    }


    @RequestMapping(path = "stop")
    public ObjectNode stop(@RequestBody ObjectNode request) {
        String queueName = request.get("queue").asText();
        if (QUEUE_Delivery.equals(queueName)) {
            poller4Delivery.stop();
        } else if (QUEUE_Conversion.equals(queueName)) {
            poller4Conversion.stop();
        } else if (QUEUE_Repeat.equals(queueName)) {
            poller4Repeat.stop();
        }

        return objectMapper.createObjectNode();
    }

    @RequestMapping(path = "revert")
    public ObjectNode revert(@RequestBody ObjectNode request) {
        BasicPoller<?> poller;

        String queueName = request.get("queue").asText();
        if (QUEUE_Delivery.equals(queueName)) {
            poller = poller4Delivery;
        } else if (QUEUE_Conversion.equals(queueName)) {
            poller = poller4Conversion;
        } else if (QUEUE_Repeat.equals(queueName)) {
            poller = poller4Repeat;
        } else {
            throw new ManageQueueException(String.format("Queue %s is not found!", queueName));
        }

        try {
            BasicQueue queue = poller.revertOne();
            return objectMapper.createObjectNode()
                    .put("result", RESULT_SUCCESS)
                    .put("messageId", queue.getId().toString());

        } catch (ManageQueueException e) {
            LOG.error(e.getMessage());

            return objectMapper.createObjectNode()
                    .put("result", RESULT_ERROR)
                    .put("error", e.getMessage());
        }
    }

    @RequestMapping(path = "resume")
    public ObjectNode resume(@RequestBody ObjectNode request) {
        String queueName = request.get("queue").asText();
        if (QUEUE_Delivery.equals(queueName)) {
            poller4Delivery.resume();
        } else if (QUEUE_Conversion.equals(queueName)) {
            //poller4Conversion.resume();
        } else if (QUEUE_Repeat.equals(queueName)) {
            //poller4Repeat.resume();
        }

        return objectMapper.createObjectNode();
    }


    @RequestMapping(path = "process")
    public ObjectNode process(@RequestBody ObjectNode request) {
        String queueName = request.get("queue").textValue();

        BasicQueue queue;

        if (QUEUE_Delivery.equals(queueName)) {
            queue = poller4Delivery.processOne();
        } else if (QUEUE_Conversion.equals(queueName)) {
            queue = poller4Conversion.processOne();
        } else if (QUEUE_Repeat.equals(queueName)) {
            queue = poller4Repeat.processOne();
        } else {
            throw new IllegalArgumentException("Queue must be specified!");
        }

        if (queue.isProcessed()) {
            return objectMapper.createObjectNode()
                    .put("result", RESULT_SUCCESS)
                    .put("id", queue.getId().toString());

        } else {
            ObjectNode response = objectMapper.createObjectNode()
                    .put("result", RESULT_ERROR)
                    .put("id", queue.getId().toString());

            if (QUEUE_Delivery.equals(queueName)) {
                error4DeliveryRepository.getFirstByQueueOrderByTimestampDesc((Queue4Delivery) queue)
                        .ifPresent(error -> response.put("error", error.getError()));
            } else if (QUEUE_Conversion.equals(queueName)) {
                error4ConversionRepository.getFirstByQueueOrderByTimestampDesc((Queue4Conversion) queue)
                        .ifPresent(error -> response.put("error", error.getError()));
            } else if (QUEUE_Repeat.equals(queueName)) {
                error4RepeatRepository.getFirstByQueueOrderByTimestampDesc((Queue4Repeat) queue)
                        .ifPresent(error -> response.put("error", error.getError()));
            }

            return response;
        }
    }


    @RequestMapping(path = "list")
    public ObjectNode list(@RequestBody ObjectNode request) {
        ObjectNode response = objectMapper.createObjectNode();

        QueueManage queueManage;
        String queueName = request.get("queue").textValue();
        if (QUEUE_Delivery.equals(queueName)) {
            queueManage = queue4DeliveryRepository;
        } else if (QUEUE_Conversion.equals(queueName)) {
            queueManage = queue4ConversionRepository;
        } else if (QUEUE_Repeat.equals(queueName)) {
            queueManage = queue4RepeatRepository;
        } else {
            throw new IllegalArgumentException("Queue must be specified!");
        }

        Pageable pageable = PageRequest.of(0, request.get("limit").asInt());

        boolean stuck = request.get("stuck").asBoolean();
        List<BasicQueue> list = stuck
                ? queueManage.getByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc(pageable)
                : queueManage.getByProcessedIsFalseOrderByTimestampAsc(pageable);

        if (request.get("processed").asBoolean()) {
            list.addAll(queueManage.getByProcessedIsTrueOrderByTimestampDesc(pageable));
        }

        ArrayNode listNode = objectMapper.createArrayNode();
        response.set("list", listNode);

        for (BasicQueue queue: list) {
            listNode.add(objectMapper.createObjectNode()
                    .put("id",        queue.getId().toString())
                    .put("timestamp", queue.getTimestamp().toString())
                    .put("size",      queue.getSize())
                    .put("attempt",   queue.getAttempt())
                    .put("stuck",     queue.isStuck())
                    .put("processed", queue.isProcessed())
                    .put("delivered", Optional.ofNullable(queue.getDeliveredTimestamp()).map(Instant::toString).orElse(null))
            );
        }

        return response;
    }

    @RequestMapping(path = "read")
    public ObjectNode read(@RequestBody ObjectNode request) {
        ObjectNode response = objectMapper.createObjectNode();

        UUID id = getId(request);

        String field = null;
        boolean fieldMessage = false;

        Function<BasicError, String> getErrorField = null;

        if (request.get("field") != null) {
            field = request.get("field").textValue();

            boolean fieldError = "error".equals(field);
            boolean fieldStackTrace = "stackTrace".equals(field);
            if (fieldError || fieldStackTrace) {
                getErrorField = error ->
                    fieldError ? error.getError() : error.getStackTrace();
            }

            fieldMessage = "message".equals(field);
        }


        BasicQueue queue = null;

        Optional<Queue4Delivery> foundDelivery = queue4DeliveryRepository.findById(id);
        if (foundDelivery.isPresent()) {
            if (getErrorField != null) {
                response.put(
                        "fieldValue",
                        error4DeliveryRepository
                            .getFirstByQueueOrderByTimestampDesc(foundDelivery.get())
                            .map(getErrorField)
                            .orElse("")
                );

            } else {
                queue = foundDelivery.get();
            }

        } else {
            Optional<Queue4Conversion> foundConversion = queue4ConversionRepository.findById(id);
            if (foundConversion.isPresent()) {
                if (getErrorField != null) {
                    response.put(
                            "fieldValue",
                            error4ConversionRepository
                                    .getFirstByQueueOrderByTimestampDesc(foundConversion.get())
                                    .map(getErrorField)
                                    .orElse("")
                    );
                } else {
                    queue = foundConversion.get();
                }

            } else {
                Optional<Queue4Repeat> foundRepeat = queue4RepeatRepository.findById(id);
                if (foundRepeat.isPresent()) {
                    if (getErrorField != null) {
                        response.put(
                                "fieldValue",
                                error4RepeatRepository
                                        .getFirstByQueueOrderByTimestampDesc(foundRepeat.get())
                                        .map(getErrorField)
                                        .orElse("")
                        );
                    } else {
                        queue = foundRepeat.get();
                    }

                } else {
                    throw new IllegalArgumentException("Сообщение по id не найдено ни в одной очереди!");
                }
            }
        }

        if (getErrorField == null) {
            if (fieldMessage) {
                response.put("fieldValue", queue.getMessage());

            } else if ("sourceId".equals(field)) {
                response.put("fieldValue", ((Queue4Delivery) queue).getSourceMessageId().toString());

            } else {

            }
        }

        return response;
    }

    @RequestMapping(path = "reconvert")
    public ObjectNode reconvert(@RequestBody ObjectNode request) {
        try {
            UUID id = getId(request);
            Queue4Delivery queue = queue4DeliveryRepository.findById(id).orElseThrow(
                    () -> new ManageQueueException("Message (" + id + ") not found in delivery queue!"));

            if (queue.isProcessed()) {
                throw new ManageQueueException("Message (" + id + ") is already processed!");
            }

            History4Delivery history = poller4Delivery.reconvert(queue);

            return objectMapper.createObjectNode()
                    .put("result", RESULT_SUCCESS)
                    .put("historyId",        history.getId().toString())
                    .put("historyTimestamp", history.getTimestamp().toString())
                    .put("newMessage",       history.getQueue().getMessage());

        } catch (ManageQueueSkipException e) {
            LOG.error(e.getMessage());

            return objectMapper.createObjectNode()
                    .put("result", RESULT_SKIPPED)
                    .put("error", e.getMessage());

        } catch (ManageQueueException e) {
            LOG.error(e.getMessage());

            return objectMapper.createObjectNode()
                    .put("result", RESULT_ERROR)
                    .put("error", e.getMessage());

        }
    }

    @RequestMapping(path = "repeat")
    public ObjectNode repeat(@RequestBody ObjectNode request) {
        UUID id = getId(request);

        try {
            Queue4Delivery queueDelivery = queue4DeliveryRepository.findById(id).orElse(null);
            if (queueDelivery != null) {
                if (queueDelivery.isProcessed()) {
                    throw new ManageQueueSkipException("Message (" + id + ") is already processed!");
                }

                id = queueDelivery.getSourceMessageId();
            }

            Queue4Conversion queueConversion = queue4ConversionRepository.findById(id).orElse(null);
            if (queueConversion == null) {
                throw new ManageQueueException("Message (" + id + ") is not found in queues: delivery, conversion!");
            }

            UUID systemId = new E2Update(queueConversion.getMessage()).systemUid();

            Queue4Repeat queueRepeat = BaseEntity.newElement(Queue4Repeat.class);
            queueRepeat.setTimestamp(timestampService.get());
            queueRepeat.setMessage(queueConversion.getMessage());
            queueRepeat.setSize(queueConversion.getSize());
            queueRepeat.setSourceMessageId(id);
            queueRepeat.setDestination(
                    systemInstanceRepository
                            .findById(systemId)
                            .orElseThrow(() -> new ManageQueueException("System (" + systemId + ") not found!"))
            );
            queue4RepeatRepository.save(queueRepeat);

            if (!queueConversion.isProcessed()) {
                queueConversion.setProcessed(true);
                queueConversion.setStuck(false);
                queue4ConversionRepository.save(queueConversion);
            }

            List<Queue4Delivery> list = queue4DeliveryRepository.findBySourceMessageId(id);
            for (Queue4Delivery qd: list) {
                if (!qd.isProcessed()) {
                    qd.setProcessed(true);
                    qd.setStuck(false);
                    queue4DeliveryRepository.save(qd);
                }
            }

            return objectMapper.createObjectNode()
                    .put("result", RESULT_SUCCESS);

        } catch (ManageQueueSkipException e) {
            LOG.error(e.getMessage());

            return objectMapper.createObjectNode()
                    .put("result", RESULT_SKIPPED)
                    .put("error", e.getMessage());

        } catch (ManageQueueException e) {
            LOG.error(e.getMessage());

            return objectMapper.createObjectNode()
                    .put("result", RESULT_ERROR)
                    .put("error", e.getMessage());

        }

    }

    private UUID getId(ObjectNode request) {
        try {
            return objectMapper.readValue(request.get("id").textValue(), UUID.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String QUEUE_Delivery   = "delivery";
    private static final String QUEUE_Conversion = "conversion";
    private static final String QUEUE_Repeat     = "repeat";

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_SKIPPED = "SKIPPED";
    private static final String RESULT_ERROR   = "ERROR";
}