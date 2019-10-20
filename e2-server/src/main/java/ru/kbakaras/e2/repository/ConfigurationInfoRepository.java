package ru.kbakaras.e2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kbakaras.e2.model.ConfigurationInfo;

import java.util.List;
import java.util.UUID;

public interface ConfigurationInfoRepository extends JpaRepository<ConfigurationInfo, UUID> {

    List<ConfigurationInfo> findByShaAndSize(String sha, int size);

}