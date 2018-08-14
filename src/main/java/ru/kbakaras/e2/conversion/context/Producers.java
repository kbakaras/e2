package ru.kbakaras.e2.conversion.context;

import java.util.LinkedList;

class Producers {
    protected LinkedList<Producer> producers = new LinkedList<>();

    public void add(Producer producer) {
        producers.add(producer);
    }

    public void make(ConversionContext4Producer ccp) {
        producers.forEach(producer -> producer.make(ccp));
    }
}