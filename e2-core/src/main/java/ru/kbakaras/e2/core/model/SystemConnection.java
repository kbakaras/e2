package ru.kbakaras.e2.core.model;

import ru.kbakaras.e2.message.E2Request;
import ru.kbakaras.e2.message.E2Response;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import java.util.UUID;

/**
 * Абстрактный класс, от которого должны наследоваться классы, отвечающие за
 * подключение систем, участвующих в обмене данными через e2.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class SystemConnection {

    public final UUID systemId;
    public final String systemName;
    public final Class<? extends SystemType> systemType;


    public SystemConnection(UUID systemId, String systemName, Class<? extends SystemType> systemType) {
        this.systemId   = systemId;
        this.systemName = systemName;
        this.systemType = systemType;
    }


    public final void sendUpdate(E2Update update) {
        sendUpdateMessage(update);
    }

    public final void sendRepeat(E2Update update) {
        sendRepeatMessage(update);
    }

    public final E2Response sendRequest(E2Request request) {

        try {

            return sendRequestMessage(request);

        } catch (Exception e) {

            return new E2Response(request.requestType())
                    .addSystemResponse(systemId.toString(), systemName)
                    .addSystemError().setText(ExceptionUtils.getMessage(e))
                    .parent.parent;

        }

    }


    protected abstract void sendUpdateMessage(E2Update update);

    protected void sendRepeatMessage(E2Update update) {
        throw new UnsupportedOperationException("Message repeat capability is not implemented for this system");
    }

    protected abstract E2Response sendRequestMessage(E2Request request);


    @Override
    public String toString() {
        return systemName;
    }

}