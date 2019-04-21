package ru.kbakaras.e2.model;

import org.dom4j.Element;
import ru.kbakaras.e2.message.E2;
import ru.kbakaras.e2.message.E2Response;
import ru.kbakaras.e2.message.Use;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import java.util.UUID;

public abstract class SystemAccessor {
    public final SystemInstance systemInstance;
    public final Class<? extends SystemType> systemType;

    public SystemAccessor(SystemInstance systemInstance, Class<? extends SystemType> systemType) {
        this.systemInstance = systemInstance;
        this.systemType = systemType;
    }


    /**
     * Отправляет запрос к системе в понятном для неё формате, возвращает ответ в формате системы.
     * Этот функционал вынесен в отдельный метод для того, чтобы его можно было запускать в отдельном потоке.
     * @param request Запрос в формате донора.
     * @return Ответ от донора в формате донора.
     */
    public Element request(Element request) {
        try {
            return doRequest(request);
        } catch (Exception e) {
            Element error = Use.createRoot(E2.ERROR, E2.NS);
            error.setText(ExceptionUtils.getMessage(e));
            return error;
        }
    }

    public void update(Element update) {
        doRequest(update);
    }

    abstract protected Element doRequest(Element donorRequest);


    public Element convertRequest(Element request) {
        return request;
    }
    public E2Response convertResponse(Element response) {
        return new E2Response(response);
    }


    /**
     * Метод для упрощения доступа к идентификатору системы
     */
    public UUID getId() {
        return systemInstance.getId();
    }

    /**
     * Метод для упрощения доступа к наименованию системы
     */
    public String getName() {
        return systemInstance.getName();
    }


    @Override
    public int hashCode() {
        return systemInstance.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return systemInstance.equals(obj);
    }

    @Override
    public String toString() {
        return systemInstance.toString();
    }
}