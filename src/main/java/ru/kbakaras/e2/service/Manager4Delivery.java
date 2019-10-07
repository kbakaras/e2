package ru.kbakaras.e2.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.manage.DestinationStat;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repository.Queue4DeliveryRepository;
import ru.kbakaras.jpa.BaseEntity;

import javax.annotation.Resource;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class Manager4Delivery implements InitializingBean, DisposableBean {

    @Resource
    private Queue4DeliveryRepository queue4DeliveryRepository;

    @Resource
    private TimestampService timestampService;

    private Map<SystemInstance, DestinationStat> stats;


    @Override
    public void afterPropertiesSet() throws Exception {
        stats = queue4DeliveryRepository.getDeliveryStats().stream()
                .collect(Collectors.toMap(stat -> stat.destination, stat -> stat));
    }

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

    public synchronized void setDelivered(Queue4Delivery queue) {
        queue.setDelivered();
        queue.setProcessed(true);
        queue4DeliveryRepository.save(queue);

        stats.get(queue.getDestination()).processedInc();
    }

    @Override
    public void destroy() throws Exception {}

}