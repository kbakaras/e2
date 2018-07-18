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
@Table(name = "route_request")
public class RouteRequest extends ProperEntity implements IReg<UUID> {
    @ManyToOne
    private SystemInstance requestor;
    @ManyToOne
    private SystemInstance responder;

    private String requestorEntityName;

    public SystemInstance getRequestor() {
        return requestor;
    }
    public void setRequestor(SystemInstance requestor) {
        this.requestor = requestor;
    }

    public SystemInstance getResponder() {
        return responder;
    }
    public void setResponder(SystemInstance responder) {
        this.responder = responder;
    }

    public String getRequestorEntityName() {
        return requestorEntityName;
    }
    public void setRequestorEntityName(String requestorEntityName) {
        this.requestorEntityName = requestorEntityName;
    }

    @Override
    public String toString() {
        return requestor + ": " + requestorEntityName + " --> " + responder;
    }

    @Override
    public boolean equivalent(Object obj) {
        if (obj instanceof RouteRequest) {
            RouteRequest route = (RouteRequest) obj;
            return requestor.equals(route.requestor) &&
                    responder.equals(route.responder) &&
                    requestorEntityName.equals(route.requestorEntityName);
        }
        return false;
    }

    public static Regset<RouteRequest> regset(List<RouteRequest> list) {
        return Regset.create(list, Regset::replaceValueNoop);
    }
}