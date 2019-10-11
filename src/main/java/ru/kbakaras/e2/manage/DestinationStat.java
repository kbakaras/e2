package ru.kbakaras.e2.manage;

import lombok.Getter;
import ru.kbakaras.e2.model.SystemInstance;

import java.time.Instant;

public class DestinationStat {

    public final SystemInstance destination;

    @Getter
    private long unprocessed;
    @Getter
    private long stuck;
    @Getter
    private long processed;
    @Getter
    private long undelivered;

    @Getter
    private boolean inFlight;

    @Getter
    private Instant timestamp;
    @Getter
    private Instant deliveredTimestamp;


    @SuppressWarnings("WeakerAccess")
    public DestinationStat(SystemInstance destination,
                           long unprocessed, long stuck, long processed, long undelivered,
                           Instant timestamp, Instant deliveredTimestamp) {

        this.destination = destination;
        this.unprocessed = unprocessed;
        this.stuck = stuck;
        this.processed = processed;
        this.undelivered = undelivered;
        this.timestamp = timestamp;
        this.deliveredTimestamp = deliveredTimestamp;

    }

    public DestinationStat(SystemInstance destination) {
        this(destination, 0, 0, 0, 0, null, null);
    }


    public DestinationStat unprocessedInc() {
        this.unprocessed++;
        return this;
    }

    public DestinationStat processedInc() {
        this.unprocessed--;
        this.processed++;
        return this;
    }

    public DestinationStat stuckInc() {
        this.stuck++;
        return this;
    }

    public DestinationStat stuckDec() {
        this.stuck--;
        return this;
    }


    public DestinationStat setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public DestinationStat setDeliveredTimestamp(Instant deliveredTimestamp) {
        this.deliveredTimestamp = deliveredTimestamp;
        return this;
    }


    public DestinationStat setInFlight(boolean inFlight) {
        this.inFlight = inFlight;
        return this;
    }

}