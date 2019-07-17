package ru.kbakaras.e2.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "configuration_reference")
@Getter @Setter
public class ConfigurationReference {

    @ManyToOne
    @JoinColumn(name = "configuration_info_id")
    private ConfigurationInfo info;

    @Column(name = "file_name")
    private String fileName;

    /**
     * Дата создания ссылки
     */
    @Column(nullable = false)
    private Instant created;

}