package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BasicQueue4Delivery extends ProperEntity {
    @Column(unique = true, nullable = false)
    private Instant timestamp;

    private UUID sourceMessageId;

    @ManyToOne
    private SystemInstance destination;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT False")
    private boolean processed;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT False")
    private boolean delivered;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT False")
    private boolean stuck;

    @Column(nullable = false, columnDefinition = "int DEFAULT 0")
    private int attempt;

    /**
     * Размер сообщения в байтах
     */
    @Column(nullable = false, columnDefinition = "bigint DEFAULT 0")
    private long size;

    public Instant getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isProcessed() {
        return processed;
    }
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isDelivered() {
        return delivered;
    }
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

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

    public boolean isStuck() {
        return stuck;
    }
    public void setStuck(boolean stuck) {
        this.stuck = stuck;
    }

    public int getAttempt() {
        return attempt;
    }
    public void incAttempt() {
        this.attempt++;
    }

    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
}