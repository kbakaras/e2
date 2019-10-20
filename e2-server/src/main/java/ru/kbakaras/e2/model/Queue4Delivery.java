package ru.kbakaras.e2.model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "queue4delivery")
public class Queue4Delivery extends BasicQueue4Delivery {
    protected Queue4Delivery() {}
}