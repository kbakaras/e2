package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.core.conversion.Converter4Payload;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Error4Conversion;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repository.Error4ConversionRepository;
import ru.kbakaras.e2.repository.Queue4ConversionRepository;
import ru.kbakaras.e2.repository.QueueManage;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.lazy.MapCache;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class Poller4Conversion extends BasicPoller<Queue4Conversion> {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Conversion.class);

    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private Error4ConversionRepository error4ConversionRepository;

    @Resource private TimestampService           timestampService;

    @Resource
    private Manager4Delivery manager4Delivery;

    @Resource private ConfigurationManager configurationManager;


    /**
     * Обработка update-сообщения. Метод сохраняет сообщение в очередь на конвертацию
     * и принудительно запускает обработку очереди.
     * @param request XML-сообщение <i>updateRequest</i>.
     */
    public void updateRequest(Element request) {
        Queue4Conversion queue = BaseEntity.newElement(Queue4Conversion.class);
        queue.setTimestamp(timestampService.get());
        queue.setMessage(request.asXML());
        queue.setSize(queue.getMessage().length());
        queue4ConversionRepository.save(queue);
        processPoll();
    }

    synchronized public void resume() {
        if (!isPolling()) {
            queue4ConversionRepository.getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc()
                    .ifPresent(queue -> {
                        queue.setStuck(false);
                        queue4ConversionRepository.save(queue);
                    });
            start();
        }
    }


    @Override
    protected Optional<Queue4Conversion> next() {
        return queue4ConversionRepository.getFirstByProcessedIsFalseOrderByTimestampAsc();
    }

    @Override
    protected QueueManage getQueueManager() {
        return queue4ConversionRepository;
    }

    @Override
    protected void process(Queue4Conversion queue) {

        Configuration4E2 conf = configurationManager.getConfiguration();

        try {
            convert(new E2Update(DocumentHelper.parseText(queue.getMessage()).getRootElement()),
                    queue.getId(),
                    conf
            );
            queue.setDelivered();
            queue.setProcessed(true);
            queue.setConfigurationReference(conf.configurationReference);

        } catch (Throwable e) {
            queue.incAttempt();
            queue.setStuck(true);

            Error4Conversion error = BaseEntity.newElement(Error4Conversion.class);
            error.setQueue(queue);
            error.setError(ExceptionUtils.getMessage(e));
            error.setStackTrace(ExceptionUtils.getStackTrace(e));
            error.setConfigurationReference(conf.configurationReference);
            error4ConversionRepository.save(error);

            LOG.error("Conversion error!{}", error);
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
    private void convert(E2Update update, UUID sourceMessageId, Configuration4E2 conf) {

        //SystemAccessor sourceAccessor = accessorRegistry.get(update.systemUid());
        //SystemAccessor sourceAccessor = conf.getSystemAccessor(update.systemUid());

        UUID   sourceId   = update.systemUid();
        String sourceName = conf.getSystemInstance(sourceId).getName();

        LOG.info("Converting message {} from system {}:", sourceMessageId, sourceName);

        /*
          Кэш накапливает результаты конвертации исходного сообщения для каждой
          результирующей системы, по мере необходимости.
         */
        MapCache<UUID, Converter4Payload> converters = MapCache.of(
                destinationId -> new Converter4Payload(
                        update,
                        new E2Update()
                                .setSystemUid(sourceId)
                                .setSystemName(sourceName),
                        conf.getConversions(sourceId, destinationId)
                )
        );

        /*
          Цикл перебирает все сущности исходного сообщения, для которых имеются
          изменённые элементы, и запускает конверсию для каждого такого элемента.
          Результаты накапливаются в кэше converters.
         */
        for (E2Entity entity: update.entities()) {
            List<E2Element> elementsChanged = entity.elementsChanged();
            if (!elementsChanged.isEmpty()) {
                for (UUID destinationId: conf.getUpdateDestinations(sourceId, entity.entityName())) {
                    Converter4Payload converter = converters.get(destinationId);
                    elementsChanged.forEach(converter::convertElement);
                }
            }
        }


        Map<UUID, Converter4Payload> results = converters.getMap();
        if (results.isEmpty()) {
            LOG.info("Message with id {} produced no results.", sourceMessageId);
            return;
        }


        /*
          Обработка результатов. Полученные в результате конвертации сообщения
          помещаются в очередь для отправки.
         */
        results.forEach((destinationId, convertedUpdate) -> {

            SystemInstance destinationInstance = conf.getSystemInstance(destinationId);

            if (!convertedUpdate.output.entities().isEmpty()) {

                manager4Delivery.add4delivery(
                        destinationInstance,
                        sourceMessageId,
                        (E2Update) convertedUpdate.output
                );

            } else {
                LOG.info("Message with id {} produced result with no entities for destination {}.",
                        sourceMessageId, destinationInstance);
            }

        });

    }

    /*private List<SystemInstance> getDestinations(SystemInstance sourceSystem, String sourceEntityName) {
        return routeUpdateRepository.getBySourceAndSourceEntityName(sourceSystem, sourceEntityName).stream()
                .map(RouteUpdate::getDestination)
                .collect(Collectors.toList());
    }*/

}