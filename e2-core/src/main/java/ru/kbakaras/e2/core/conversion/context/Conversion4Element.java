package ru.kbakaras.e2.core.conversion.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2ElementUse;
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
    private static final Logger LOG = LoggerFactory.getLogger(Conversion4Element.class);

    private String entityName;
    private String elementUid;

    private Boolean changed;
    private Boolean deleted;
    private Function<ConversionContext4Element, Boolean> deletedFunction;
    private boolean synth;
    private E2ElementUse use;

    private Producers producers = new Producers();

    public final Producers4Attributes attributes = new Producers4Attributes(producers);
    public final Producers4Tables tables     = new Producers4Tables(producers);


    public Conversion4Element entity(String entityName) {
        this.entityName = entityName;
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

    public Conversion4Element synth(boolean synth) {
        this.synth = synth;
        return this;
    }

    public Conversion4Element use(E2ElementUse use) {
        this.use = use;
        return this;
    }


    public Producers4Attributes.Producer4Attribute attributes(String attributeName) {
        return attributes.attribute(attributeName);
    }

    public Producers4Tables.Producer4Table tables(String tableName) {
        return tables.table(tableName);
    }


    E2Element make(ConversionContext cc, Consumer<E2Element> resultSetter) {
        if (entityName == null) {
            entityName = cc.conversion.getDefaultDestinationEntity();
        }
        if (entityName == null || entityName.isEmpty()) {
            throw new E2Exception4Write("Empty entity is not allowed!");
        }

        LOG.debug("{} Producing -------------------------------", entityName);

        E2Entity entity = cc.converter.output.createEntity(entityName);

        E2Element destinationElement = entity
                .addElement(elementUid != null ? elementUid : cc.sourceElement.getUid())
                .setChanged(changed    != null ? changed    : cc.sourceElement.isChanged())
                .setDeleted(deleted    != null ? deleted    : cc.sourceElement.isDeleted())
                .setSynth(synth)
                .setUse(use);

        resultSetter.accept(destinationElement);

        ConversionContext4Element  cce = new ConversionContext4Element(cc, cc.sourceElement, destinationElement);
        ConversionContext4Producer ccp = new ConversionContext4Producer(cce, cc.sourceElement.attributes, destinationElement.attributes);

        if (deletedFunction != null) {
            destinationElement.setDeleted(deletedFunction.apply(cce));
        }

        LOG.debug("{} Applying automatic attribute conversions:", entityName);
        attributes.makeAuto(ccp);
        LOG.debug("{} Applying automatic table conversions:", entityName);
        tables.makeAuto(ccp);
        LOG.debug("{} Applying other producers:", entityName);
        producers.make(ccp);
        LOG.debug("{} -----------------------------------------", entityName);

        return destinationElement;
    }
}