package ru.kbakaras.e2.service;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.manage.DestinationStat;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repository.Error4DeliveryRepository;
import ru.kbakaras.e2.repository.Queue4DeliveryRepository;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Manager4Delivery implements InitializingBean, DisposableBean {

    @Resource
    private Queue4DeliveryRepository queue4DeliveryRepository;
    @Resource
    private Error4DeliveryRepository error4DeliveryRepository;

    @Resource
    private TimestampService timestampService;
    @Resource
    private ConfigurationManager configurationManager;

    private Map<SystemInstance, DestinationStat> stats;

    private ExecutorService executor;


    @Override
    public void afterPropertiesSet() {

        stats = queue4DeliveryRepository.getDeliveryStats().stream()
                .collect(Collectors.toMap(stat -> stat.destination, stat -> stat));

        executor = Executors.newSingleThreadExecutor();

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
        return stat.getStuck() == 0 && !stat.isInFlight() && stat.getUnprocessed() > 0;
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
    @Transactional(propagation = Propagation.REQUIRED)
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
    @Transactional(propagation = Propagation.REQUIRED)
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
    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void checkStuck(Queue4Delivery queue, Throwable deliveryException) {

        DestinationStat stat = stats.get(queue.getDestination())
                .setInFlight(false);

        queue.incAttempt();
        if (queue.getAttempt() >= ATTEMPT_MAX) {
            queue.setStuck(true);
            stat.stuckInc();
        }
        queue4DeliveryRepository.save(queue);

        Error4Delivery error = BaseEntity.newElement(Error4Delivery.class);
        error.setQueue(queue);
        error.setError(ExceptionUtils.getMessage(deliveryException));
        error.setStackTrace(ExceptionUtils.getStackTrace(deliveryException));
        error4DeliveryRepository.save(error);
        error4DeliveryRepository.flush(); // Если этого не сделать, то при второй попытке возникнет OptimisticLock

        log.error("Update delivery error!{}", error);

        if (possibleToRunDelivery(stat)) {
            runDelivery(stat, queue);
        }

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void unstuck(Queue4Delivery queue) {
        queue.setStuck(false);
        queue4DeliveryRepository.save(queue);
        stats.get(queue.getDestination()).stuckDec();
    }


    public synchronized List<DestinationStat> getStats() {
        return stats.values().stream()
                .sorted(Comparator.comparing(st -> st.destination.getName()))
                .collect(Collectors.toList());
    }


    @Override
    public void destroy() throws Exception {}


    private static final int ATTEMPT_MAX = 3;

}