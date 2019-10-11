package ru.kbakaras.e2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kbakaras.e2.manage.DestinationStat;
import ru.kbakaras.e2.model.Queue4Delivery;
import ru.kbakaras.e2.model.SystemInstance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Queue4DeliveryRepository extends JpaRepository<Queue4Delivery, UUID>, QueueManage {

    Optional<Queue4Delivery> getFirstByProcessedIsFalseAndStuckIsFalseOrderByTimestampAsc();
    Optional<Queue4Delivery> getFirstByProcessedIsFalseOrderByTimestampAsc();


    /**
     * Получение следующего необработанного сообщения для указанной системы назначения
     */
    Optional<Queue4Delivery> getFirstByDestinationAndProcessedIsFalseOrderByTimestampAsc(SystemInstance destination);

    Optional<Queue4Delivery> getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc();
    List<Queue4Delivery> findBySourceMessageId(UUID sourceMessageId);

    @Query("SELECT NEW ru.kbakaras.e2.manage.DestinationStat( "                                      +
            "q.destination, "                                                                        +
            "SUM(CASE WHEN q.processed = TRUE THEN 0 ELSE 1 END), "                                  +
            "SUM(CASE WHEN q.processed = FALSE AND q.stuck = TRUE THEN 1 ELSE 0 END), "              +
            "SUM(CASE WHEN q.processed = TRUE THEN 1 ELSE 0 END), "                                  +
            "SUM(CASE WHEN q.processed = TRUE AND q.deliveredTimestamp IS NULL THEN 1 ELSE 0 END), " +
            "MAX(q.timestamp), "                                                                     +
            "MAX(q.deliveredTimestamp) ) "                                                           +
            "FROM Queue4Delivery AS q "                                                              +
            "GROUP BY q.destination")
    List<DestinationStat> getDeliveryStats();

}