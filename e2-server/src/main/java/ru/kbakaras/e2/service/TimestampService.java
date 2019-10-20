package ru.kbakaras.e2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TimestampService {
    private static final Logger LOG = LoggerFactory.getLogger(TimestampService.class);

    private Instant last = now();

    synchronized public Instant get() {
        Instant now = now();
        if (now.equals(last)) {
            now = now.plusMillis(1);
            LOG.info("Timestamp collision prevented");
        }
        return last = now;
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}