package ru.kbakaras.e2.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "error4repeat")
public class Error4Repeat extends BasicError {
    @ManyToOne
    private Queue4Repeat queue;

    protected Error4Repeat() {}

    public Queue4Repeat getQueue() {
        return queue;
    }
    public void setQueue(Queue4Repeat queue) {
        this.queue = queue;
    }
}