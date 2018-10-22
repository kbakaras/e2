package ru.kbakaras.e2.manage;

public class QueueStats {
    public final long processed;
    public final long delivered;
    public final long count;
    public final long stuck;
    public final boolean stopped;

    public QueueStats(long processed, long delivered, long count, long stuck, boolean stopped) {
        this.processed = processed;
        this.delivered = delivered;
        this.count     = count;
        this.stuck     = stuck;
        this.stopped   = stopped;
    }
}
