package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;
import ru.kbakaras.jpa.Regset;
import ru.kbakaras.sugar.entity.IReg;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "route_update")
public class RouteUpdate extends ProperEntity implements IReg<UUID> {
    @ManyToOne
    private SystemInstance source;
    @ManyToOne
    private SystemInstance destination;

    private String sourceEntityName;

    public SystemInstance getSource() {
        return source;
    }
    public void setSource(SystemInstance source) {
        this.source = source;
    }

    public SystemInstance getDestination() {
        return destination;
    }
    public void setDestination(SystemInstance destination) {
        this.destination = destination;
    }

    public String getSourceEntityName() {
        return sourceEntityName;
    }
    public void setSourceEntityName(String sourceEntityName) {
        this.sourceEntityName = sourceEntityName;
    }

    @Override
    public String toString() {
        return source + ": " + sourceEntityName + " --> " + destination;
    }

    @Override
    public boolean equivalent(Object obj) {
        if (obj instanceof RouteUpdate) {
            RouteUpdate route = (RouteUpdate) obj;
            return source.equals(route.source) &&
                    destination.equals(route.destination) &&
                    sourceEntityName.equals(route.sourceEntityName);
        }
        return false;
    }

    public static Regset<RouteUpdate> regset(List<RouteUpdate> list) {
        return Regset.create(list, Regset::replaceValueNoop);
    }
}