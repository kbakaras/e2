package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.SystemType;

import java.util.UUID;

public interface SystemTypeRepository extends JpaRepository<SystemType, UUID> {}
