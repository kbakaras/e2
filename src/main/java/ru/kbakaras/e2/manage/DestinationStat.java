package ru.kbakaras.e2.manage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.kbakaras.e2.model.SystemInstance;

import java.time.Instant;

@AllArgsConstructor
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

    @Getter @Setter
    private Instant timestamp;
    @Getter @Setter
    private Instant deliveredTimestamp;


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

}