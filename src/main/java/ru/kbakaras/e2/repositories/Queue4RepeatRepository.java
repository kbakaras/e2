package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Queue4Repeat;

import java.util.Optional;
import java.util.UUID;

public interface Queue4RepeatRepository extends JpaRepository<Queue4Repeat, UUID> {
    Optional<Queue4Repeat> getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc();
    Optional<Queue4Repeat> getFirstByProcessedIsFalseOrderByTimestampAsc();
}