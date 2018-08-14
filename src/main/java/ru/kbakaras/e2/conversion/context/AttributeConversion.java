package ru.kbakaras.e2.conversion.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;
import ru.kbakaras.e2.message.E2Scalar;

import java.util.function.Function;

public class AttributeConversion extends AttributeProducer {
    private static final Logger LOG = LoggerFactory.getLogger(AttributeConversion.class);

    private String sourceName;
    private String destinationName;

    private String explicitEntity;
    private Function<E2Scalar, E2Scalar> conversion;

    public AttributeConversion(String sourceName, String destinationName) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
    }

    public AttributeConversion convert(Function<E2Scalar, E2Scalar> conversion) {
        this.conversion = conversion;
        return this;
    }

    public AttributeConversion convertString(Function<String, String> conversion) {
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
    public AttributeConversion explicitEntity(String explicitEntity) {
        this.explicitEntity = explicitEntity;
        return this;
    }


    private E2AttributeValue applyConversion(E2AttributeValue value, Converter4Payload converter) {
        if (value instanceof E2Scalar) {
            return applyConversion((E2Scalar) value);
        } else if (value instanceof E2Reference) {
            return applyConversion((E2Reference) value, converter);
        } else {
            throw new E2Exception4Write("Unknown attribute value type!");
        }
    }
    private E2AttributeValue applyConversion(E2Scalar value) {
        return conversion != null ? conversion.apply(value) : value;
    }
    private E2AttributeValue applyConversion(E2Reference value, Converter4Payload converter) {
        if (conversion != null) {
            LOG.warn("Conversion is not applicable for reference-valued attributes! Conversion ignored.");
        }
        return converter.input.referencedElement(value)
                .map(converter::convertElement)
                .map(converted -> explicitEntity != null ? converted.get(explicitEntity) : converted.get())
                .orElseThrow(() -> new E2Exception4Write("Possibly, explicitEntity is wrong, or you need to provide it!"));
    }

    @Override
    public void make(ConversionContext4Producer ccp) {
        ccp.sourceAttributes.get(sourceName).ifPresent(
                sourceAttribute -> applyConversion(sourceAttribute.attributeValue(), ccp.parent.parent.converter)
                        .apply(ccp.destinationAttributes.add(destinationName)));
    }
}