package ru.kbakaras.e2.service.rest;

public class ManageQueueException extends RuntimeException {
    public ManageQueueException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ManageQueueException(String s) {
        super(s);
    }
}
