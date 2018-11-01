package ru.kbakaras.e2.service;

import org.springframework.stereotype.Service;
import ru.kbakaras.e2.manage.QueueStats;
import ru.kbakaras.e2.model.Error4Delivery;
import ru.kbakaras.e2.repositories.Error4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4ConversionRepository;
import ru.kbakaras.e2.repositories.Queue4DeliveryRepository;
import ru.kbakaras.e2.repositories.Queue4RepeatRepository;
import ru.kbakaras.e2.repositories.QueueManage;

import javax.annotation.Resource;

@Service
public class ManageService {
    @Resource private Queue4ConversionRepository queue4ConversionRepository;
    @Resource private Poller4Conversion poller4Conversion;

    @Resource private Queue4DeliveryRepository queue4DeliveryRepository;
    @Resource private Error4DeliveryRepository error4DeliveryRepository;
    @Resource private Poller4Delivery poller4Delivery;

    @Resource private Queue4RepeatRepository queue4RepeatRepository;
    @Resource private Poller4Repeat poller4Repeat;

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
                statsRepository.countByProcessedIsTrueAndDeliveredIsTrue(),
                statsRepository.countByProcessedIsFalse(),
                statsRepository.countByProcessedIsFalseAndStuckIsTrue(),
                !poller.isPolling()
        );
    }


    public Error4Delivery getErrorStuck() {
        return queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc()
                .flatMap(q -> error4DeliveryRepository.getFirstByQueueOrderByTimestampDesc(q))
                .orElse(null);
    }

}