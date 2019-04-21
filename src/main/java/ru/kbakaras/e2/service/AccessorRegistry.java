package ru.kbakaras.e2.service;

import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.SystemAccessor;
import ru.kbakaras.e2.model.SystemInstance;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Service
public class AccessorRegistry {

    public SystemAccessor get(SystemInstance systemInstance) {
        return null;
    }

    public SystemAccessor get(UUID systemUid) {
        return null;
    }

    public Set<SystemAccessor> get(UUID[] systemUids) {
        return get(Arrays.asList(systemUids));
    }

    public Set<SystemAccessor> get(Collection<UUID> systemUids) {
        return null;
    }
}