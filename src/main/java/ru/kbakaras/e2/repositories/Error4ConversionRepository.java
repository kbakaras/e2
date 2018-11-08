package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Error4Conversion;
import ru.kbakaras.e2.model.Queue4Conversion;

import java.util.Optional;
import java.util.UUID;

public interface Error4ConversionRepository extends JpaRepository<Error4Conversion, UUID> {
    Optional<Error4Conversion> getFirstByQueueOrderByTimestampDesc(Queue4Conversion queue);
}