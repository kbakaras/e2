package ru.kbakaras.e2.message;

public interface E2Referring<R> {
    R setReference(String entityName, String elementUid);
    R setValue(String value);
}