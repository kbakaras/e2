package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.Error4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.Optional;

@Service
public class Poller4Delivery extends BasicPoller<Queue4Delivery> {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Delivery.class);

    private boolean stopOnStuck = true;

    @Resource private Queue4DeliveryRepository queue4DeliveryRepository;
    @Resource private Error4DeliveryRepository error4DeliveryRepository;

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

    @Override
    protected Optional<Queue4Delivery> next() {
        if (stopOnStuck) {
            return queue4DeliveryRepository.getFirstByProcessedIsFalseOrderByTimestampAsc();
        } else {
            return queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc();
        }
    }

    @Override
    protected void process(Queue4Delivery queue) {
        try {
            Element update = DocumentHelper.parseText(queue.getMessage()).getRootElement();
            SystemInstance destination = queue.getDestination();

            destination.update(destination.getType().convertRequest(update));
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