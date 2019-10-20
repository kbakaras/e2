package ru.kbakaras.e2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import ru.kbakaras.e2.model.BasicQueue;
import ru.kbakaras.e2.repository.QueueManage;
import ru.kbakaras.e2.service.rest.ManageQueueException;

import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BasicPoller<Q extends BasicQueue> implements InitializingBean, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(BasicPoller.class);

    private Timer timer;
    private long timerDelay = 10000;
    private final Lock lock = new ReentrantLock();

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void afterPropertiesSet() {
        start();
    }

    synchronized public void start() {
        if (timer == null) {
            String pollerName = getPollerName();
            LOG.info("Starting {}...", pollerName);
            timer = new Timer(pollerName);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    processPoll();
                }
            }, 0, timerDelay);
        }
    }

    synchronized public void stop() {
        timer.cancel();
        timer = null;
        LOG.info("{} STOPPED", this.getClass().getSimpleName());
    }

    public boolean isPolling() {
        return timer != null;
    }

    protected abstract void process(Q message);
    protected abstract Optional<Q> next();

    protected abstract QueueManage getQueueManager();

    /**
     * Метод вызывается таймером обработчика очереди. Выполняет попытку обработать
     * первое сообщение очереди, если оно не отмечено как застрявшее (stuck).<br/>
     * Если же попадается застрявшее сообщение, метод останавливает таймер, очередь
     * перестаёт обрабатывать сообщения.<br/><br/>
     * Клиентский код также может вызвать этот метод. Как правило, при помещении
     * следующего сообщения в очередь, чтобы не ждать срабатывания по таймеру. Это,
     * в каком-то смысле, оптимизация для случая, когда очередь пуста и есть
     * возможность обработать новое сообщение сразу.
     */
    public final void processPoll() {
        if (lock.tryLock()) {
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Processing {}...", this.getClass().getSimpleName());
                }

                Optional<Q> found;
                while ((found = next()).isPresent()) {
                    if (!found.get().isStuck()) {
                        process(found.get());

                    } else {
                        if (timer != null) {
                            LOG.warn("Message stuck! Stopping queue {}", found.get().getClass().getSimpleName());
                            stop();

                        } else {
                            LOG.warn("Message stuck on queue {}!", found.get().getClass().getSimpleName());
                        }

                        break;
                    }
                }

            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Метод выполняет обработку одного сообщения в остановленной очереди
     */
    synchronized public Q processOne() {
        if (isPolling()) {
            throw new ManageQueueException(
                    "Poller is active! It's not possible to forcibly process.");
        }

        Q queue = next().orElseThrow(
                () -> new ManageQueueException(String.format(
                        "Next message not found in [%s]!", getPollerName())));

        process(queue);

        return queue;
    }

    /**
     * Метод выполняет отмену обработки одного последнего сообщения в остановленной очереди
     */
    @SuppressWarnings("unchecked")
    synchronized public Q revertOne() {
        if (isPolling()) {
            throw new ManageQueueException(
                    "Poller is active! It's not possible to forcibly process.");
        }

        QueueManage repository = getQueueManager();

        List<BasicQueue> queueList = repository.getByProcessedIsTrueOrderByTimestampDesc(PageRequest.of(0, 1));

        if (queueList.isEmpty()) {
            throw new ManageQueueException(String.format(
                    "Last processed message not found in [%s]!", getPollerName()));
        }

        BasicQueue queue = queueList.get(0);

        queue.setDeliveredTimestamp(null);
        queue.setProcessed(false);
        ((CrudRepository) repository).save(queue);

        return (Q) queue;
    }

    protected String getPollerName() {
        return this.getClass().getSimpleName();
    }
}