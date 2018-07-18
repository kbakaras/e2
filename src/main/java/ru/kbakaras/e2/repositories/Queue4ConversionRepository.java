package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Queue4Conversion;

import java.util.Optional;
import java.util.UUID;

public interface Queue4ConversionRepository extends JpaRepository<Queue4Conversion, UUID> {
    Optional<Queue4Conversion> getFirstByProcessedIsFalseOrderByTimestampAsc();
}