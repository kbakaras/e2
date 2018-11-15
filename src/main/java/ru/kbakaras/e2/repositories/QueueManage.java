package ru.kbakaras.e2.repositories;

import org.springframework.data.domain.Pageable;
import ru.kbakaras.e2.model.BasicQueue;

import java.util.List;

public interface QueueManage {
    Long countByProcessedIsTrue();
    Long countByProcessedIsTrueAndDeliveredTimestampIsNotNull();
    Long countByProcessedIsFalse();
    Long countByProcessedIsFalseAndStuckIsTrue();

    List<BasicQueue> getByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc(Pageable pageable);
    List<BasicQueue> getByProcessedIsFalseOrderByTimestampAsc(Pageable pageable);

    List<BasicQueue> getByProcessedIsTrueOrderByTimestampDesc(Pageable pageable);
}