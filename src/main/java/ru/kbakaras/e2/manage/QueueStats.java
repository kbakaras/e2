package ru.kbakaras.e2.manage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueueStats {

    public final long processed;
    public final long delivered;
    public final long count;
    public final long stuck;
    public final boolean stopped;

}