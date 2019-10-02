package ru.kbakaras.e2.manage;

import lombok.RequiredArgsConstructor;
import ru.kbakaras.e2.model.SystemInstance;

import java.time.Instant;

@RequiredArgsConstructor
public class DestinationStat {

    public final SystemInstance destination;

    public final long unprocessed;
    public final long stuck;
    public final long processed;
    public final long undelivered;

    public final Instant timestamp;
    public final Instant deliveredTimestamp;

}