package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "error4delivery")
public class Error4Delivery extends ProperEntity {
    @ManyToOne
    private Queue4Delivery queue;

    @Column(columnDefinition = "text")
    private String error;

    @Column(columnDefinition = "text")
    private String stackTrace;

    private Instant timestamp;

    public Error4Delivery() {
        timestamp = Instant.now();
    }

    public Queue4Delivery getQueue() {
        return queue;
    }
    public void setQueue(Queue4Delivery queue) {
        this.queue = queue;
    }

    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }

    public String getStackTrace() {
        return stackTrace;
    }
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}