package ru.kbakaras.e2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Error4Repeat;
import ru.kbakaras.e2.model.Queue4Repeat;

import java.util.Optional;
import java.util.UUID;

public interface Error4RepeatRepository extends JpaRepository<Error4Repeat, UUID> {
    Optional<Error4Repeat> getFirstByQueueOrderByTimestampDesc(Queue4Repeat queue);
}