package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "queue4conversion")
public class Queue4Conversion extends ProperEntity {
    @Column(unique = true, nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT False")
    private boolean processed;

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
}