package ru.kbakaras.e2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.kbakaras.e2.manage.DestinationStat;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repository.Error4DeliveryRepository;
import ru.kbakaras.e2.repository.Queue4DeliveryRepository;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.UUID;
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

    private Map<SystemInstance, DestinationStat> stats;


    @Override
    public void afterPropertiesSet() throws Exception {
        stats = queue4DeliveryRepository.getDeliveryStats().stream()
                .collect(Collectors.toMap(stat -> stat.destination, stat -> stat));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void add4delivery(SystemInstance destinationInstance, UUID sourceMessageId, E2Update message) {

        Queue4Delivery queue = BaseEntity.newElement(Queue4Delivery.class);
        queue.setMessage(message.xml().asXML());
        queue.setSize(queue.getMessage().length());
        queue.setTimestamp(timestampService.get());
        queue.setSourceMessageId(sourceMessageId);
        queue.setDestination(destinationInstance);
        queue4DeliveryRepository.save(queue);

        stats.computeIfAbsent(queue.getDestination(), DestinationStat::new)
                .unprocessedInc();

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void setDelivered(Queue4Delivery queue) {
        queue.setDelivered();
        queue.setProcessed(true);
        queue4DeliveryRepository.save(queue);

        stats.get(queue.getDestination()).processedInc();
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void checkStuck(Queue4Delivery queue, Throwable e) {

        queue.incAttempt();
        if (queue.getAttempt() >= ATTEMPT_MAX) {
            queue.setStuck(true);
            stats.get(queue.getDestination()).stuckInc();
        }
        queue4DeliveryRepository.save(queue);

        Error4Delivery error = BaseEntity.newElement(Error4Delivery.class);
        error.setQueue(queue);
        error.setError(ExceptionUtils.getMessage(e));
        error.setStackTrace(ExceptionUtils.getStackTrace(e));
        error4DeliveryRepository.save(error);

        log.error("Update delivery error!{}", error);

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void unstuck(Queue4Delivery queue) {
        queue.setStuck(false);
        queue4DeliveryRepository.save(queue);
        stats.get(queue.getDestination()).stuckDec();
    }


    @Override
    public void destroy() throws Exception {}


    private static final int ATTEMPT_MAX = 3;

}