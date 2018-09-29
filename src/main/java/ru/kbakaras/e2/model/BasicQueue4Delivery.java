package ru.kbakaras.e2.model;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class BasicQueue4Delivery extends BasicQueue {
    private UUID sourceMessageId;

    @ManyToOne
    private SystemInstance destination;

    public UUID getSourceMessageId() {
        return sourceMessageId;
    }
    public void setSourceMessageId(UUID sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public SystemInstance getDestination() {
        return destination;
    }
    public void setDestination(SystemInstance destination) {
        this.destination = destination;
    }
}