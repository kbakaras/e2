package ru.kbakaras.e2.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.kbakaras.e2.model.History4Delivery;
import ru.kbakaras.e2.model.Queue4Delivery;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class HistoryService {
    @PersistenceContext private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRED)
    public History4Delivery reconverted(Queue4Delivery queue, String newMessage) {
        History4Delivery history = History4Delivery.newElement(queue);
        em.persist(history);

        queue.setMessage(newMessage);
        queue.setSize(newMessage.length());
        em.merge(queue);

        return history;
    }
}
