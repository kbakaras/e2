package ru.kbakaras.e2.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "error4conversion")
public class Error4Conversion extends BasicError {
    @ManyToOne
    private Queue4Conversion queue;

    protected Error4Conversion() {}

    public Queue4Conversion getQueue() {
        return queue;
    }
    public void setQueue(Queue4Conversion queue) {
        this.queue = queue;
    }
}
