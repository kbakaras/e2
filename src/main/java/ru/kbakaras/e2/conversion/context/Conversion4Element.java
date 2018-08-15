package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Exception4Write;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Класс, позволяющий настроить экземпляр для получения одного
 * результирующего элемента (со всеми результирующими атрибутами
 * и табличными частями) на основании контекста конверсии.
 */
public class Conversion4Element {
    private String entityName;
    private Consumer<E2Entity> entityInitializer;
    private String elementUid;

    private Boolean changed;
    private Boolean deleted;
    private Function<ConversionContext4Element, Boolean> deletedFunction;

    private Producers producers = new Producers();

    public final Producers4Attributes attributes = new Producers4Attributes(producers);
    public final Producers4Tables     tables     = new Producers4Tables(producers);


    public Conversion4Element entity(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public Conversion4Element entity(Consumer<E2Entity> entityInitializer) {
        this.entityInitializer = entityInitializer;
        return this;
    }

    public Conversion4Element entity(String entityName, Consumer<E2Entity> entityInitializer) {
        this.entityName = entityName;
        this.entityInitializer = entityInitializer;
        return this;
    }


    public Conversion4Element uid(String elementUid) {
        this.elementUid = elementUid;
        return this;
    }

    public Conversion4Element changed(boolean changed) {
        this.changed = changed;
        return this;
    }

    public Conversion4Element deleted(Function<ConversionContext4Element, Boolean> deletedFunction) {
        this.deletedFunction = deletedFunction;
        return this;
    }

    public Conversion4Element deleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }


    public Producers4Attributes.Producer4Attribute attributes(String attributeName) {
        return attributes.attribute(attributeName);
    }

    public Producers4Tables.Producer4Table tables(String tableName) {
        return tables.table(tableName);
    }


    E2Element make(ConversionContext cc) {
        if (entityName == null) {
            entityName = cc.conversion.getDefaultDestinationEntity();
        }
        if (entityName == null || entityName.isEmpty()) {
            throw new E2Exception4Write("Empty entity is not allowed!");
        }

        E2Entity entity = cc.converter.output.createEntity(
                entityName,
                entityInitializer);

        E2Element destinationElement = entity
                .addElement(elementUid != null ? elementUid : cc.sourceElement.getUid())
                .setChanged(changed    != null ? changed    : cc.sourceElement.isChanged())
                .setDeleted(deleted    != null ? deleted    : cc.sourceElement.isDeleted());

        ConversionContext4Element  cce = new ConversionContext4Element(cc, cc.sourceElement, destinationElement);
        ConversionContext4Producer ccp = new ConversionContext4Producer(cce, cc.sourceElement.attributes, destinationElement.attributes);

        if (deletedFunction != null) {
            destinationElement.setDeleted(deletedFunction.apply(cce));
        }

        attributes.makeAuto(ccp);
        tables.makeAuto(ccp);
        producers.make(ccp);

        return destinationElement;
    }
}