package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Queue4Delivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Queue4DeliveryRepository extends JpaRepository<Queue4Delivery, UUID>, QueueManage {
    Optional<Queue4Delivery> getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc();
    Optional<Queue4Delivery> getFirstByProcessedIsFalseOrderByTimestampAsc();

    Optional<Queue4Delivery> getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc();
    List<Queue4Delivery> findBySourceMessageId(UUID sourceMessageId);
}