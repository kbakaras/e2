package ru.kbakaras.e2.service.rest;

public class ExchangeException extends RuntimeException {
    public ExchangeException(String message) {
        super(message);
    }

    public ExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
