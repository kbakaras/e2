package ru.kbakaras.e2.repositories;

public interface QueueManage {
    Long countByProcessedIsTrue();
    Long countByProcessedIsTrueAndDeliveredIsTrue();
    Long countByProcessedIsFalse();
    Long countByProcessedIsFalseAndStuckIsTrue();
}
