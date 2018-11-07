package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.Error4Repeat;
import ru.kbakaras.e2.model.Queue4Repeat;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.Error4RepeatRepository;
import ru.kbakaras.e2.repositories.Queue4RepeatRepository;
import ru.kbakaras.jpa.BaseEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.Optional;

@Service
public class Poller4Repeat extends BasicPoller<Queue4Repeat> {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Repeat.class);

    private boolean stopOnStuck = true;

    @Resource private Queue4RepeatRepository queue4RepeatRepository;
    @Resource private Error4RepeatRepository error4RepeatRepository;

    @Override
    protected Optional<Queue4Repeat> next() {
        if (stopOnStuck) {
            return queue4RepeatRepository.getFirstByProcessedIsFalseOrderByTimestampAsc();
        } else {
            return queue4RepeatRepository.getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc();
        }
    }

    @Override
    protected void process(Queue4Repeat queue) {
        try {
            Element update = DocumentHelper.parseText(queue.getMessage()).getRootElement();
            SystemInstance destination = queue.getDestination();

            destination.update(destination.getType().convertRequest(update));
            queue.setDelivered();
            queue.setProcessed(true);

        } catch (Throwable e) {
            LOG.error("Repeat delivery error!");

            queue.incAttempt();
            if (queue.getAttempt() >= ATTEMPT_MAX) {
                queue.setStuck(true);
            }

            Error4Repeat error = BaseEntity.newElement(Error4Repeat.class);
            error.setQueue(queue);
            error.setError(ExceptionUtils.getMessage(e));
            error.setStackTrace(ExceptionUtils.getStackTrace(e));
            error4RepeatRepository.save(error);

            LOG.error(error.getError());
        }

        queue4RepeatRepository.save(queue);
    }

    private static final int ATTEMPT_MAX = 3;
}