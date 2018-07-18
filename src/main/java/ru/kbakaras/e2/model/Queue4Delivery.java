package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "queue4delivery")
public class Queue4Delivery extends ProperEntity {
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
    private boolean stuck;

    @Column(nullable = false, columnDefinition = "int DEFAULT 0")
    private int attempt;

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
}