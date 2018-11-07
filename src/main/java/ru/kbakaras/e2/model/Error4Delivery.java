package ru.kbakaras.e2.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "error4delivery")
public class Error4Delivery extends BasicError {
    @ManyToOne
    private Queue4Delivery queue;

    protected Error4Delivery() {}

    public Queue4Delivery getQueue() {
        return queue;
    }
    public void setQueue(Queue4Delivery queue) {
        this.queue = queue;
    }
}