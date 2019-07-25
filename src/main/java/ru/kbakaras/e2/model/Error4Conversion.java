package ru.kbakaras.e2.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter @Setter
@Entity @Table(name = "error4conversion")
public class Error4Conversion extends BasicError {

    @ManyToOne
    private Queue4Conversion queue;

    @ManyToOne @JoinColumn(name = "configuration_reference_id")
    private ConfigurationReference configurationReference;

}