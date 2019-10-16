package ru.kbakaras.e2.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.core.conversion.Converter4Payload;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.manage.DestinationStat;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.model.History4Delivery;
import ru.kbakaras.e2.model.Queue4Conversion;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repository.Error4DeliveryRepository;
import ru.kbakaras.e2.repository.Queue4ConversionRepository;
import ru.kbakaras.e2.repository.Queue4DeliveryRepository;
import ru.kbakaras.e2.service.rest.ManageQueueException;
import ru.kbakaras.e2.service.rest.ManageQueueSkipException;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Manager4Delivery implements InitializingBean, DisposableBean {

    @Resource
    private Queue4DeliveryRepository queue4DeliveryRepository;
    @Resource
    private Queue4ConversionRepository queue4ConversionRepository;
    @Resource
    private Error4DeliveryRepository error4DeliveryRepository;

    @Resource
    private HistoryService historyService;

    @Resource
    private TimestampService timestampService;
    @Resource
    private ConfigurationManager configurationManager;

    private Map<SystemInstance, DestinationStat> stats;

    private ExecutorService executor;

    @Getter
    private volatile boolean active;


    @Override
    public void afterPropertiesSet() {

        stats = queue4DeliveryRepository.getDeliveryStats().stream()
                .collect(Collectors.toMap(stat -> stat.destination, stat -> stat));

        executor = Executors.newSingleThreadExecutor();
        start();

    }

    /**
     * Выполняет доставку одного сообщения. Выполняется в отдельном потоке. О результате доставки сигнализирует
     * вызовом одного из методов: {@link #setDelivered(Queue4Delivery)}
     * или {@link #checkStuck(Queue4Delivery, Throwable)}.
     *
     * @param queue Сообщение для доставки
     */
    private void process(Queue4Delivery queue) {
        Configuration4E2 conf = configurationManager.getConfiguration();

        try {

            E2Update update = new E2Update(
                    DocumentHelper
                            .parseText(queue.getMessage())
                            .getRootElement()
            );
            SystemConnection connection = conf.getSystemConnection(queue.getDestination().getId());

            connection.sendUpdate(update);

            setDelivered(queue);

        } catch (Throwable e) {

            checkStuck(queue, e);

        }

    }


    private boolean possibleToRunDelivery(DestinationStat stat) {
        return active && stat.getStuck() == 0 && !stat.isInFlight() && stat.getUnprocessed() > 0;
    }

    private synchronized void runDelivery(DestinationStat stat) {
        queue4DeliveryRepository.getFirstByDestinationAndProcessedIsFalseOrderByTimestampAsc(stat.destination)
                .ifPresent(queue -> {
                    stat.setInFlight(true);
                    executor.execute(() -> process(queue));
                });
    }

    private synchronized void runDelivery(DestinationStat stat, Queue4Delivery queue) {
        stat.setInFlight(true);
        executor.execute(() -> process(queue));
    }


    /**
     * Добавляет сообщение в очередь на доставку. Это происходит в результате конвертации очередного сообщения
     * очереди на конвертацию.<br/><br/>
     * Метод сохраняет сообщение в базу, инкрементирует счётчик в статистике и, если это возможно, запускает
     * процесс доставки следующего сообщения для данной системы.
     *
     * @param destinationInstance Система назначения
     * @param sourceMessageId     Идентификатор исходного сообщения, конвертация которого привела к возникновению
     *                            данного сообщения для доставки
     * @param message             Само сообщение на доставку
     */
    public synchronized void add4delivery(SystemInstance destinationInstance, UUID sourceMessageId, E2Update message) {

        Queue4Delivery queue = BaseEntity.newElement(Queue4Delivery.class);
        queue.setMessage(message.xml().asXML());
        queue.setSize(queue.getMessage().length());
        queue.setTimestamp(timestampService.get());
        queue.setSourceMessageId(sourceMessageId);
        queue.setDestination(destinationInstance);
        queue4DeliveryRepository.save(queue);

        DestinationStat stat = stats.computeIfAbsent(queue.getDestination(), DestinationStat::new)
                .unprocessedInc()
                .setTimestamp(queue.getTimestamp());


        if (possibleToRunDelivery(stat)) {
            runDelivery(stat);
        }

    }

    /**
     * Помечает указанное сообщение как доставленное. Вызывается из потока, выполняющего доставку.<br/><br/>
     *
     * Метод записывает сообщение в базу с обновлением признака доставки. Обновляет статистику очереди.
     * Если это возможно, запускает доставку следующего сообщения для данной системы.
     *
     * @param queue Доставленное сообщение
     */
    public synchronized void setDelivered(Queue4Delivery queue) {

        queue.setDelivered();
        queue.setProcessed(true);
        queue4DeliveryRepository.save(queue);

        DestinationStat stat = stats.get(queue.getDestination())
                .processedInc()
                .setDeliveredTimestamp(queue.getDeliveredTimestamp())
                .setInFlight(false);


        if (possibleToRunDelivery(stat)) {
            runDelivery(stat);
        }

    }

    /**
     * Выполняется из потока, выполняющего доставку, в случае сбоя доставки. Выполняет проверку на исчерпание
     * количества попыток доставки и, если необходимо помечает сообщение и очередь для данной системы как
     * "застрявшее".<br/><br/>
     *
     * Метод выполняет обновление количества попыток отправки в сообщении, а также признак "застрявшее",
     * сохраняет эти изменения в базу. После этого записывает в базу ошибку доставки в привязке к данному сообщению.
     * Вносит изменения в статистику очереди. Если не исчерпаны попытки запускает повтор доставки данного сообщения.
     *
     * @param queue             Сообщение, отправка которого не удалась
     * @param deliveryException Исключение, возникшее в результате неудачной попытки отправки
     */
    public synchronized void checkStuck(Queue4Delivery queue, Throwable deliveryException) {

        DestinationStat stat = stats.get(queue.getDestination())
                .setInFlight(false);

        queue.incAttempt();
        if (queue.getAttempt() >= ATTEMPT_MAX) {
            queue.setStuck(true);
            stat.stuckInc();
        }
        queue = queue4DeliveryRepository.save(queue);

        Error4Delivery error = BaseEntity.newElement(Error4Delivery.class);
        error.setQueue(queue);
        error.setError(ExceptionUtils.getMessage(deliveryException));
        error.setStackTrace(ExceptionUtils.getStackTrace(deliveryException));
        error4DeliveryRepository.save(error);

        log.error("Update delivery error!{}", error);

        if (possibleToRunDelivery(stat)) {
            runDelivery(stat, queue);
        }

    }

    public synchronized void unstuck(Queue4Delivery queue) {
        queue.setStuck(false);
        queue4DeliveryRepository.save(queue);
        stats.get(queue.getDestination()).stuckDec();
    }


    /**
     * Выполняет повторную конвертацию для указанного сообщения очереди доставки. Конвертироваться
     * будет исходное сообщение из очереди конвертации, на которое имеется ссылка в данном сообщении. Конвертация
     * будет выполнена только для системы-получателя, которой было предназначено данное сообщение очереди
     * на доставку.<br/><br/>
     *
     * Конвертация возможна, только если обработка сообщений на доставку для соответствующего получателя
     * в данный момент остановлена. Для этого должна быть либо отключена обработка всей очереди на доставку
     * (методом {@link #stop()}), либо очередь для данного получателя остановлена автоматически по причине
     * <i>застревания</i> следующего сообщения.<br/><br/>
     *
     * Повторная конвертация возможна только для необработанных сообщений (флаг processed == false).
     *
     * @param queue Сообщение из очереди на доставку, содержимое которого будет обновлено путём выполнения
     *              конвертации его исходного сообщения из очереди на конвертацию.
     * @return Если повторная конвертация выполняется, создаётся запись в таблице истории, привязанная к данному
     * сообщению очереди на доставку. Элемент такой записи метод вернёт.
     */
    public synchronized History4Delivery reconvert(Queue4Delivery queue) {

        DestinationStat stat = stats.get(queue.getDestination());

        if (active && stat.getStuck() == 0) {
            throw new ManageQueueException(MessageFormat.format(
                    "Delivery is active for destination {0}! It's not possible to reconvert.",
                    stat.destination));
        }

        if (queue.isProcessed()) {
            throw new ManageQueueException(MessageFormat.format(
                    "Message for destination {0} with id {1} already processed! It's not possible to reconvert.",
                    queue.getDestination(), queue.getId()));
        }

        Configuration4E2 conf = configurationManager.getConfiguration();

        Queue4Conversion sourceQueue = queue4ConversionRepository.getOne(queue.getSourceMessageId());

        E2Update sourceMessage = new E2Update(sourceQueue.getMessage());

        UUID        sourceId   = sourceMessage.systemUid();
        String      sourceName = conf.getSystemInstance(sourceId).getName();
        UUID   destinationId   = queue.getDestination().getId();
        String destinationName = conf.getSystemInstance(destinationId).getName();

        log.info("Reconverting message {} from system {} for system {}:",
                sourceQueue.getId(), sourceName, destinationName);


        Converter4Payload converter = new Converter4Payload(sourceMessage,
                new E2Update()
                        .setSystemUid(sourceId.toString())
                        .setSystemName(sourceName),
                conf.getConversions(sourceId, destinationId)
        );


        for (E2Entity entity: sourceMessage.entities()) {
            List<E2Element> elementsChanged = entity.elementsChanged();
            if (!elementsChanged.isEmpty() && conf.updateRouteExists(sourceId, destinationId, entity.entityName())) {
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


    public synchronized List<DestinationStat> getStats() {
        return stats.values().stream()
                .sorted(Comparator.comparing(st -> st.destination.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Останавливает отправку сообщений для всех получателей. Если в момент вызова
     * данного метода выполняется отправка каких-либо сообщений, она не будет прервана,
     * просто новые отправки не будут стартовать.
     */
    public synchronized void stop() {
        this.active = false;
    }

    /**
     * Снова запускает остановленную методом {@link #stop()} отправку сообщений для всех систем,
     * для которых это возможно.
     */
    public synchronized void start() {

        if (!active) {
            active = true;

            // Запуск отправки во все системы
            stats.values().stream()
                    .filter(stat -> stat.getStuck() == 0 && stat.getUnprocessed() > 0)
                    .map(stat -> stat.destination)
                    .map(queue4DeliveryRepository::getFirstByDestinationAndProcessedIsFalseOrderByTimestampAsc)
                    .forEach(result -> result.ifPresent(queue -> {
                        stats.get(queue.getDestination()).setInFlight(true);
                        executor.execute(() -> process(queue));
                    }));
        }

    }


    @Override
    public synchronized void destroy() throws InterruptedException {
        stop();
        executor.shutdown();
        executor.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.MINUTES);
    }


    private static final int ATTEMPT_MAX = 3;
    private static final long TERMINATION_TIMEOUT = 2; // В минутах

}