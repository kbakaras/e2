package ru.kbakaras.e2.conversion.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;
import ru.kbakaras.e2.message.E2Scalar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class Conversion4Attribute extends Producer {
    private static final Logger LOG = LoggerFactory.getLogger(Conversion4Attribute.class);

    private String sourceName;
    private String destinationName;
    private Function<ConversionContext4Producer, E2Attribute> attributeCreator;

    private String explicitEntity;
    private Function<E2Scalar, E2Scalar> conversion;
    private String dereference;

    private Set<String> skip4input  = new HashSet<>();
    private Set<String> skip4output = new HashSet<>();

    Conversion4Attribute(String sourceName, String destinationName, Function<ConversionContext4Producer, E2Attribute> attributeCreator) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.attributeCreator = attributeCreator;
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
     * Метод позволяет настроить разыменование для ссылочного поля. При этом
     * по ссылочному полю находится сам элемент исходного сообщения, а в элементе
     * ужё берётся указанный реквизит.<br/>
     * В будущем нужно подумать над другим вариантом этого функционала, так как
     * сейчас он покрывает только один конкретный случай. Могут понадобиться последовательные
     * разыменования с последующими конверсиями.
     * @param dereference Реквизит, который будет взят из разыменованного элемента
     *                    в качестве входного значения.
     */
    public Conversion4Attribute dereference(String dereference) {
        this.dereference = dereference;
        return this;
    }

    public Conversion4Attribute skip4input(String... values) {
        skip4input.addAll(Arrays.asList(values));
        return this;
    }
    public Conversion4Attribute skip4output(String... values) {
        skip4output.addAll(Arrays.asList(values));
        return this;
    }

    public Conversion4Attribute skipFalse4input() {
        return skip4input("false", "FALSE", "False");
    }
    public Conversion4Attribute skipFalse4output() {
        return skip4output("false", "FALSE", "False");
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


    @Override
    void make(ConversionContext4Producer ccp) {
        LOG.debug("Applying attribute conversion {} --> {}", sourceName, destinationName);

        ccp.sourceAttributes.get(sourceName)
                .map(E2Attribute::attributeValue)
                .flatMap(value -> {
                    if (value instanceof E2Scalar) {
                        if (dereference != null) {
                            LOG.warn("Dereference is not applicable for scalar-valued attributes! Dereference ignored.");
                        }

                        return Optional.of((E2Scalar) value)
                                .filter(scalar -> !skip4input.contains(scalar.string()))
                                .map(scalar -> conversion != null ? conversion.apply(scalar) : scalar)
                                .filter(scalar -> !skip4output.contains(scalar.string()))
                                .map(scalar -> (E2AttributeValue) scalar);


                    } else if (value instanceof E2Reference) {
                        if (conversion != null) {
                            LOG.warn("Conversion is not applicable for reference-valued attributes! Conversion ignored.");
                        }

                        Optional<E2Element> result = ccp.input().referencedElement((E2Reference) value);
                        if (dereference != null) {
                            result = result
                                    .flatMap(element -> element.attributes.get(dereference))
                                    .map(E2Attribute::reference)
                                    .flatMap(reference -> ccp.input().referencedElement(reference));
                        }

                        return result
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
                })
                .ifPresent(value -> value.apply(attributeCreator.apply(ccp)));
    }
}