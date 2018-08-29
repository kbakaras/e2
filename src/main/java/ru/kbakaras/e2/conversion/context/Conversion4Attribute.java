package ru.kbakaras.e2.conversion.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;
import ru.kbakaras.e2.message.E2Scalar;

import java.util.Optional;
import java.util.function.Function;

public class Conversion4Attribute extends Producer {
    private static final Logger LOG = LoggerFactory.getLogger(Conversion4Attribute.class);

    private String sourceName;
    private String destinationName;

    private String explicitEntity;
    private Function<E2Scalar, E2Scalar> conversion;

    private String defaultValue;

    Conversion4Attribute(String sourceName, String destinationName) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
    }

    public Conversion4Attribute convert(Function<E2Scalar, E2Scalar> conversion) {
        this.conversion = conversion;
        return this;
    }

    public Conversion4Attribute convertString(Function<String, String> conversion) {
        this.conversion = value -> new E2Scalar(conversion.apply(value.string()));
        return this;
    }

    /**
     * В том случае, когда конверсия применяется к ссылочному атрибуту, есть возможность,
     * что элемент, на который он ссылается, конвертируется по варианту Split (то есть разделяется
     * на две или более сущности). В таком случае, чтобы результирующему реквизиту назначить
     * ссылочное значение, нужно задать в явном виде сущность этой ссылки.<br/><br/>
     *
     * Тогда, к исходному элементу, на который ссылается данный исходный атрибут, будет
     * применена соответствующая конверсия, а из результата конверсии (объект {@link Converted})
     * будет получена конкретная ссылка для сущности explicitEntity.<br/><br/>
     *
     * Если явно сущность не указать, то конверсия попытается обойтись без неё. И она либо
     * не понадобится, либо будет выброшено исключение.
     *
     * @param explicitEntity Сущность для результирующей ссылки
     * @return
     */
    public Conversion4Attribute explicitEntity(String explicitEntity) {
        this.explicitEntity = explicitEntity;
        return this;
    }

    public Conversion4Attribute defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    void make(ConversionContext4Producer ccp) {
        Optional<E2AttributeValue> bar = ccp.sourceAttributes.get(sourceName)
                .map(E2Attribute::attributeValue)
                .flatMap(value -> {
                    if (value instanceof E2Scalar) {
                        return Optional.of(conversion != null ? conversion.apply((E2Scalar) value) : value);

                    } else if (value instanceof E2Reference) {
                        if (conversion != null) {
                            LOG.warn("Conversion is not applicable for reference-valued attributes! Conversion ignored.");
                        }

                        return ccp.input().referencedElement((E2Reference) value)
                                .map(ccp.converter()::convertElement)
                                .filter(Converted::notIgnored)
                                .map(converted -> {
                                    E2AttributeValue av = explicitEntity != null ? converted.get(explicitEntity) : converted.get();
                                    if (av == null) {
                                        throw new E2Exception4Write("Possibly, explicitEntity is wrong, or you need to provide it!");
                                    }
                                    return av;
                                });

                    } else {
                        throw new E2Exception4Write("Unknown attribute value type!");
                    }
                });

        if (bar.isPresent()) {
            bar.get().apply(ccp.destinationAttributes.add(destinationName));
        } else if (defaultValue != null) {
            ccp.destinationAttributes.add(destinationName).setValue(defaultValue);
        }
    }
}