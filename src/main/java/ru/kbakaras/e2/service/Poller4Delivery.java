package ru.kbakaras.e2.service;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.core.model.SystemConnection;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.e2.model.Configuration4E2;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.repository.Queue4DeliveryRepository;
import ru.kbakaras.e2.repository.QueueManage;

import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Service
public class Poller4Delivery extends BasicPoller<Queue4Delivery> {
    private boolean stopOnStuck = true;

    @Resource private Queue4DeliveryRepository   queue4DeliveryRepository;

    @Resource
    private Manager4Delivery manager4Delivery;

    @Resource private ConfigurationManager configurationManager;

    @Override
    public void afterPropertiesSet() {}


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

            E2Update update = new E2Update(
                    DocumentHelper
                            .parseText(queue.getMessage())
                            .getRootElement()
            );
            SystemConnection connection = conf.getSystemConnection(queue.getDestination().getId());

            connection.sendUpdate(update);

            manager4Delivery.setDelivered(queue);

        } catch (Throwable e) {

            manager4Delivery.checkStuck(queue, e);

        }

    }

}