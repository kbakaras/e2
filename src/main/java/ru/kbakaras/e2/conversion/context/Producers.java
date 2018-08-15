package ru.kbakaras.e2.conversion.context;

import java.util.LinkedList;

class Producers {
    private LinkedList<Producer> producers = new LinkedList<>();

    void add(Producer producer) {
        producers.add(producer);
    }

    void make(ConversionContext4Producer ccp) {
        producers.forEach(producer -> producer.make(ccp));
    }
}