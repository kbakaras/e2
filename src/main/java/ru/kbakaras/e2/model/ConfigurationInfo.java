package ru.kbakaras.e2.model;

import lombok.Getter;
import lombok.Setter;
import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "configuration_info")
@Getter @Setter
public class ConfigurationInfo extends ProperEntity {

    @Column(nullable = false)
    private int size;

    @Column(columnDefinition = "CHAR(40)", nullable = false)
    private String sha;

    @Column(nullable = false)
    private Instant timestamp;


    @Override
    protected void newElement() {
        super.newElement();
        timestamp = Instant.now();
    }

}