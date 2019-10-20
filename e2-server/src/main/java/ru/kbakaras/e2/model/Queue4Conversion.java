package ru.kbakaras.e2.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter @Setter
@Entity @Table(name = "queue4conversion")
public class Queue4Conversion extends BasicQueue {

    @ManyToOne @JoinColumn(name = "configuration_reference_id")
    private ConfigurationReference configurationReference;

}