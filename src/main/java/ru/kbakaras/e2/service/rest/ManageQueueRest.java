package ru.kbakaras.e2.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.kbakaras.e2.model.BasicError;
import ru.kbakaras.e2.model.BasicQueue;
import ru.kbakaras.e2.model.Error4Delivery;
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
import ru.kbakaras.e2.service.ManageService;
import ru.kbakaras.e2.service.Poller4Delivery;

import javax.annotation.Resource;
import java.io.IOException;
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
    @Resource private ManageService manageService;
    @Resource private Poller4Delivery poller4Delivery;

    @Resource private Queue4DeliveryRepository   queue4DeliveryRepository;
    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private Queue4RepeatRepository     queue4RepeatRepository;

    @Resource private Error4DeliveryRepository   error4DeliveryRepository;
    @Resource private Error4ConversionRepository error4ConversionRepository;
    @Resource private Error4RepeatRepository     error4RepeatRepository;

    @Resource private ObjectMapper objectMapper;

    @RequestMapping(path = "stats")
    public ObjectNode stats(String value) {
        return objectMapper.createObjectNode()
                .putPOJO("conversion", manageService.getConversionStats())
                .putPOJO("delivery",   manageService.getDeliveryStats())
                .putPOJO("repeat",     manageService.getRepeatStats());
    }

    @RequestMapping(path = "error")
    public ObjectNode error(@RequestBody ObjectNode request) {
        ObjectNode response = objectMapper.createObjectNode();

        boolean stuck = request.get("stuck").asBoolean();
        if (stuck) {
            Error4Delivery error = manageService.getErrorStuck();
            if (error != null) {
                try {
                    response.put("error", error.getError());
                    response.put("stackTrace", error.getStackTrace());
                    response.put("timestamp", objectMapper.writeValueAsString(error.getTimestamp()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return response;
    }

    @RequestMapping(path = "message")
    public ObjectNode message(@RequestBody ObjectNode request) {
        ObjectNode response = objectMapper.createObjectNode();

        boolean stuck = request.get("stuck").asBoolean();
        if (stuck) {
            queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc().ifPresent(msg -> {
                boolean source = request.get("source").asBoolean();

                try {
                    if (source) {
                        Queue4Conversion msgSource = queue4ConversionRepository.getOne(msg.getSourceMessageId());
                        response.put("message",   msgSource.getMessage());
                        response.put("timestamp", objectMapper.writeValueAsString(msgSource.getTimestamp()));
                    } else {
                        response.put("message",   msg.getMessage());
                        response.put("timestamp", objectMapper.writeValueAsString(msg.getTimestamp()));
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return response;
    }

    @RequestMapping(path = "resume")
    public void resume(@RequestBody ObjectNode request) {
        String queueName = request.get("queue").asText();
        if ("delivery".equals(queueName)) {
            poller4Delivery.resume();
        }
    }

    @RequestMapping(path = "list")
    public ObjectNode list(@RequestBody ObjectNode request) {
        ObjectNode response = objectMapper.createObjectNode();

        QueueManage queueManage;
        String queueName = request.get("queue").textValue();
        if ("delivery".equals(queueName)) {
            queueManage = queue4DeliveryRepository;
        } else if ("conversion".equals(queueName)) {
            queueManage = queue4ConversionRepository;
        } else if ("repeat".equals(queueName)) {
            queueManage = queue4RepeatRepository;
        } else {
            throw new IllegalArgumentException("Queue must be specified!");
        }

        Pageable pageable = PageRequest.of(0, request.get("limit").asInt());

        boolean stuck = request.get("stuck").asBoolean();
        List<BasicQueue> list = stuck
                ? queueManage.getByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc(pageable)
                : queueManage.getByProcessedIsFalseOrderByTimestampAsc(pageable);

        ArrayNode listNode = objectMapper.createArrayNode();
        response.set("list", listNode);

        for (BasicQueue queue: list) {
            listNode.add(objectMapper.createObjectNode()
                    .put("id",        queue.getId().toString())
                    .put("timestamp", queue.getTimestamp().toString())
                    .put("size",      queue.getSize())
                    .put("attempt",   queue.getAttempt())
                    .put("stuck",     queue.isStuck())
            );
        }

        return response;
    }

    @RequestMapping(path = "read")
    public ObjectNode read(@RequestBody ObjectNode request) {
        ObjectNode response = objectMapper.createObjectNode();

        UUID id;

        try {
            id = objectMapper.readValue(request.get("id").textValue(), UUID.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


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
}