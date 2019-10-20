package ru.kbakaras.e2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.ConfigurationReference;

import java.util.UUID;

public interface ConfigurationReferenceRepository extends JpaRepository<ConfigurationReference, UUID> {

    ConfigurationReference findFirstByOrderByTimestampDesc();

}