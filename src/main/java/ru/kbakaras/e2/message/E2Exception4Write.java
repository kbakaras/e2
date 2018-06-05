package ru.kbakaras.e2.message;

public class E2Exception4Write extends RuntimeException {
    public E2Exception4Write(String message) {
        super(message);
    }
    public E2Exception4Write(Exception e) {
        super(e);
    }
}
