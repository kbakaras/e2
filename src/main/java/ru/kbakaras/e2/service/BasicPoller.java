package ru.kbakaras.e2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.kbakaras.e2.model.BasicQueue;

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
        timer.cancel();
    }

    @Override
    public void afterPropertiesSet() {
        start();
    }

    synchronized public void start() {
        if (timer == null) {
            String pollerName = this.getClass().getSimpleName();
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

    public boolean isPolling() {
        return timer != null;
    }

    protected abstract void process(Q message);
    protected abstract Optional<Q> next();

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
                            timer.cancel();
                            timer = null;

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
}