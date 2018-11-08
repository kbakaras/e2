package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "history4delivery")
public class History4Delivery extends ProperEntity {
    @ManyToOne
    private Queue4Delivery queue;

    /**
     * Момент времени, когда данная версия сообщения была перенесена в историю.
     */
    @Column(unique = true, nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "text")
    private String message;

    /**
     * Размер сообщения в байтах
     */
    @Column(nullable = false, columnDefinition = "bigint DEFAULT 0")
    private long size;


    protected History4Delivery() {}

    public Queue4Delivery getQueue() {
        return queue;
    }
    public void setQueue(Queue4Delivery queue) {
        this.queue = queue;
    }

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

    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
}