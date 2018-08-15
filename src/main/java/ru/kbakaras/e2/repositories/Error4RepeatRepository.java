package ru.kbakaras.e2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.Error4Repeat;

import java.util.UUID;

public interface Error4RepeatRepository extends JpaRepository<Error4Repeat, UUID> {}