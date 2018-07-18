package ru.kbakaras.e2.model;

import org.dom4j.Element;
import ru.kbakaras.e2.message.E2;
import ru.kbakaras.e2.message.Use;
import ru.kbakaras.jpa.ProperEntity;
import ru.kbakaras.sugar.utils.ExceptionUtils;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "system_instance")
@DiscriminatorColumn(name = "did", discriminatorType = DiscriminatorType.STRING, length = 36)
public abstract class SystemInstance extends ProperEntity {
    private String name;

    @ManyToOne
    private SystemType type;

    private String url;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public SystemType getType() {
        return type;
    }
    public void setType(SystemType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
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

    @Override
    public String toString() {
        return getName();
    }
}