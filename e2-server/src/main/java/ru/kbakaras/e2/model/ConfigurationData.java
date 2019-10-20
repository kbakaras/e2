package ru.kbakaras.e2.model;

import lombok.Getter;
import lombok.Setter;
import ru.kbakaras.sugar.entity.IEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "configuration_data")
@Getter @Setter
public class ConfigurationData implements IEntity<UUID> {

    @Id
    private UUID id;

    private byte[] data;

}