package ru.kbakaras.e2.repositories;

public interface QueueManage {
    Long countByProcessedIsTrue();
    Long countByProcessedIsTrueAndDeliveredIsNotNull();
    Long countByProcessedIsFalse();
    Long countByProcessedIsFalseAndStuckIsTrue();
}
