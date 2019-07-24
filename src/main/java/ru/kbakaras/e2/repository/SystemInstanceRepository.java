package ru.kbakaras.e2.repository;

import ru.kbakaras.e2.model.SystemInstance;
import ru.kbakaras.jpa.repository.SugarRepository;

import java.util.UUID;

public interface SystemInstanceRepository extends
        SugarRepository<SystemInstance, UUID>
{}