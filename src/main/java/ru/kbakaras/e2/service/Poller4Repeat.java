package ru.kbakaras.e2.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.Error4Repeat;
import ru.kbakaras.e2.model.Queue4Repeat;
import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.e2.repositories.Error4RepeatRepository;
import ru.kbakaras.e2.repositories.Queue4RepeatRepository;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class Poller4Repeat implements InitializingBean, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(Poller4Repeat.class);

    private Timer timer;
    private final Lock lock = new ReentrantLock();

    private boolean stopOnStuck = true;

    @Resource private Queue4RepeatRepository queue4RepeatRepository;
    @Resource private Error4RepeatRepository error4RepeatRepository;

    @Override
    public void destroy() throws Exception {
        timer.cancel();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    synchronized public void start() {
        if (timer == null) {
            LOG.info("Starting repeat queue...");
            timer = new Timer("Poller4Delivery");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    process();
                }
            }, 0, 10000);
        }
    }

    private void process() {
        if (lock.tryLock()) {
            try {
                LOG.trace("Checking queue for delivery...");

                Supplier<Optional<Queue4Repeat>> supplier = stopOnStuck ?
                        queue4RepeatRepository::getFirstByProcessedIsFalseOrderByTimestampAsc :
                        queue4RepeatRepository::getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc;

                Optional<Queue4Repeat> found;
                while ((found = supplier.get()).isPresent()) {
                    if (!found.get().isStuck()) {
                        deliver(found.get());

                    } else {
                        if (timer != null) {
                            LOG.warn("Message stuck! Stopping repeat queue.");
                            timer.cancel();
                            timer = null;
                        } else {
                            LOG.warn("Message stuck!");
                        }
                        break;
                    }
                }

            } finally {
                lock.unlock();
            }
        }
    }

    private void deliver(Queue4Repeat queue) {
        try {
            Element update = DocumentHelper.parseText(queue.getMessage()).getRootElement();
            SystemInstance destination = queue.getDestination();

            destination.update(destination.getType().convertRequest(update));
            queue.setDelivered(true);
            queue.setProcessed(true);

        } catch (Throwable e) {
            LOG.error("Update delivery error!");

            queue.incAttempt();
            if (queue.getAttempt() >= ATTEMPT_MAX) {
                queue.setStuck(true);
            }

            Error4Repeat error = new Error4Repeat();
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