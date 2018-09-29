package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.RouteUpdate;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.Queue4ConversionRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.e2.repositories.RouteUpdateRepository;
import ru.kbakaras.e2.repositories.SystemInstanceRepository;
import ru.kbakaras.sugar.lazy.MapCache;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class Poller4Conversion extends BasicPoller<Queue4Conversion> {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Conversion.class);

    @Resource private SystemInstanceRepository   systemInstanceRepository;
    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private Queue4DeliveryRepository   queue4DeliveryRepository;
    @Resource private RouteUpdateRepository      routeUpdateRepository;
    @Resource private ConversionRegistry         conversionRegistry;
    @Resource private TimestampService           timestampService;

    @Override
    protected Optional<Queue4Conversion> next() {
        return queue4ConversionRepository.getFirstByProcessedIsFalseOrderByTimestampAsc();
    }

    @Override
    protected void process(Queue4Conversion queue) {
        try {
            convert(new E2Update(
                            DocumentHelper.parseText(queue.getMessage()).getRootElement()),
                    queue.getId()
            );
            queue.setDelivered(true);
            queue.setProcessed(true);

        } catch (Throwable e) {
            LOG.error("Conversion error!", e);

            queue.incAttempt();
            queue.setStuck(true);
        }

        queue4ConversionRepository.save(queue);
    }

    /**
     * Выполняет конвертацию исходного update-сообщения. При этом может произойти создание
     * нескольких выходных сообщений (в зависимости от маршрутов, прописанных для обновления
     * сущностей).<br/><br/>
     * 1. Метод перебирает все сущности исходного сообщения.<br/>
     * 2. Для каждой сущности находит все маршруты, заданные для
     * обновления данной сущности с учётом исходной системы.<br/>
     * 3. Если найдены маршруты, выполняет конверсию изменённых элементов
     * сущности для каждой целевой системы.
     *
     * @param update          Входное update-сообщение.
     * @param sourceMessageId Уникальный идентификатор входного сообщения в очереди на конвертацию.
     */
    private void convert(E2Update update, UUID sourceMessageId) {
        SystemInstance source = systemInstanceRepository.findById(update.systemUid()).get();
        // TODO: updateSystemName(sourceSystem, update.systemName());

        LOG.info("Converting message {} from system {}:", sourceMessageId, source);

        /**
         * Кэш накапливает результаты конвертации исходного сообщения для каждой
         * результирующей системы, по мере необходимости.
         */
        MapCache<SystemInstance, Converter4Payload> converters = MapCache.of(
                destination -> new Converter4Payload(
                        update,
                        new E2Update().setSystemUid(source.getId().toString()).setSystemName(source.getName()),
                        conversionRegistry.get(source.getType(), destination.getType()))
        );

        for (E2Entity entity: update.entities()) {
            List<E2Element> elementsChanged = entity.elementsChanged();
            if (!elementsChanged.isEmpty()) {
                for (SystemInstance destination: getDestinations(source, entity.entityName())) {
                    elementsChanged.forEach(converters.get(destination)::convertElement);
                }
            }
        }

        Map<SystemInstance, Converter4Payload> results = converters.getMap();
        if (!results.isEmpty()) {
            results.forEach((destination, convertedUpdate) -> {
                if (!convertedUpdate.output.entities().isEmpty()) {
                    Queue4Delivery queue = new Queue4Delivery();
                    queue.setMessage(((E2Update) convertedUpdate.output).xml().asXML());
                    queue.setSize(queue.getMessage().length());
                    queue.setTimestamp(timestampService.get());
                    queue.setSourceMessageId(sourceMessageId);
                    queue.setDestination(destination);
                    queue4DeliveryRepository.save(queue);

                } else {
                    LOG.info("Message with id {} produced result with no entities for destination {}.",
                            sourceMessageId, destination);
                }
            });

        } else {
            LOG.info("Message with id {} produced no results.", sourceMessageId);
        }
    }

    private List<SystemInstance> getDestinations(SystemInstance sourceSystem, String sourceEntityName) {
        return routeUpdateRepository.getBySourceAndSourceEntityName(sourceSystem, sourceEntityName).stream()
                .map(RouteUpdate::getDestination)
                .collect(Collectors.toList());
    }
}