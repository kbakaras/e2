package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Error4Delivery;

import java.util.UUID;

public interface Error4DeliveryRepository extends JpaRepository<Error4Delivery, UUID> {
}
