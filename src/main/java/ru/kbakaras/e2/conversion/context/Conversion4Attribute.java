package ru.kbakaras.e2.conversion.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.*;

import java.util.function.Function;

public class Conversion4Attribute extends Producer4Attribute {
    private static final Logger LOG = LoggerFactory.getLogger(Conversion4Attribute.class);

    private ConversionContext conversionContext;
    private String explicitEntity;
    private Function<E2Scalar, E2Scalar> conversion;

    Conversion4Attribute(ConversionContext conversionContext) {
        this.conversionContext = conversionContext;
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


    private E2AttributeValue applyConversion(E2AttributeValue value) {
        if (value instanceof E2Scalar) {
            return applyConversion((E2Scalar) value);
        } else if (value instanceof E2Reference) {
            return applyConversion((E2Reference) value);
        } else {
            throw new E2Exception4Write("Unknown attribute value type!");
        }
    }
    private E2AttributeValue applyConversion(E2Scalar value) {
        return conversion != null ? conversion.apply(value) : value;
    }
    private E2AttributeValue applyConversion(E2Reference value) {
        if (conversion != null) {
            LOG.warn("Conversion is not applicable for reference-valued attributes! Conversion ignored.");
        }
        return conversionContext.converter.input.referencedElement(value)
                .map(conversionContext.converter::convertElement)
                .map(converted -> explicitEntity != null ? converted.get(explicitEntity) : converted.get())
                .orElseThrow(() -> new E2Exception4Write("Possibly, explicitEntity is wrong, or you need to provide it!"));
    }

    public void apply(E2Attribute sourceAttribute, E2Attribute destinationAttribute) {
        applyConversion(sourceAttribute.attributeValue())
                .apply(destinationAttribute);
    }
}