package ru.kbakaras.e2.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Attributes;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;
import ru.kbakaras.e2.message.E2Scalar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Объект-контекст, передаваемый в специализированную конверсию.
 */
public class ConversionContext {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionContext.class);

    public final Converter4Payload converter;
    public final E2Element sourceElement;
    public final Converted converted;

    private Conversion conversion;

    private List<E2Element> destinationElements = new ArrayList<>();
    private AttributeConversion COPY = new AttributeConversion();


    public ConversionContext(Converter4Payload converter, Conversion conversion, E2Element sourceElement, Converted converted) {
        this.converter     = converter;
        this.sourceElement = sourceElement;
        this.converted     = converted;
        this.conversion    = conversion;
    }


    public DestinationSetup addResult() {
        return new DestinationSetup().addToResult();
    }
    public DestinationSetup addDestination() {
        return new DestinationSetup();
    }

    public ConversionContext setResult(E2Reference destinationReference) {
        converted.put(destinationReference);
        return this;
    }


    /**
     * Поиск в стеке контекстов атрибута <i>attributeName</i> в элементе сущности <i>sourceEntity</i>.
     * @param entityName Имя искомой сущности
     * @param attributeName Имя искомого реквизита
     * @return Первый найденный реквизит
     */
    public E2Attribute findDestinationContext(String entityName, String attributeName) {
        for (ConversionContext context: converter.contextStack()) {
            for (E2Element destinationElement: context.destinationElements) {
                if (destinationElement.entityName().equals(entityName)) {
                    Optional<E2Attribute> attribute = destinationElement.attributes.get(attributeName);
                    if (attribute.isPresent()) {
                        return attribute.get();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Класс используется для настройки и создания контекста назначения. Позволяет определить
     * сущность результирующего элемента, задать исходный элемент для копирования, определить,
     * должен ли создаваемый контекст стать результирующим для данного контекста конверсии.
     */
    public class DestinationSetup {
        private String entityName;
        private Consumer<E2Entity> entityInitializer;
        private String elementUid = sourceElement.getUid();
        private boolean addToResult = true;

        private DestinationSetup() {}

        public DestinationSetup entity(String entityName) {
            this.entityName = entityName;
            return this;
        }
        public DestinationSetup entity(Consumer<E2Entity> entityInitializer) {
            this.entityInitializer = entityInitializer;
            return this;
        }
        public DestinationSetup entity(String entityName, Consumer<E2Entity> entityInitializer) {
            this.entityName = entityName;
            this.entityInitializer = entityInitializer;
            return this;
        }

        public DestinationSetup element(String elementUid) {
            this.elementUid = elementUid;
            return this;
        }

        public DestinationSetup setAddToResult(boolean addToResult) {
            this.addToResult = addToResult;
            return this;
        }
        public DestinationSetup addToResult() {
            this.addToResult = true;
            return this;
        }
        public DestinationSetup doNotAddToResult() {
            this.addToResult = false;
            return this;
        }

        public DestinationContext ok() {
            if (entityName == null) {
                entityName = conversion.getDefaultDestinationEntity();
            }
            if (entityName == null || entityName.isEmpty()) {
                throw new E2Exception4Write("Empty entity is not allowed!");
            }

            E2Entity  entity  = converter.output.createEntity(entityName, entityInitializer);
            E2Element element = entity.addElement(elementUid);

            destinationElements.add(element);
            if (addToResult) {
                converted.put(element.asReference());
            }

            return new DestinationContext(element)
                    .setChanged(sourceElement.isChanged())
                    .setDeleted(sourceElement.isDeleted());
        }
    }

    /**
     * Класс используется для настройки конверсий для атрибутов элемента.
     * Конверсии применяются последовательно (в порядке добавления) в момент
     * вызова метода {@link DestinationContext#make()}. В момент добавления
     * конверсии просто формируется отсортированный map.<br/><br/>
     *
     * Благодаря тому, что при выполнении конверсий соблюдается тот порядок
     * в котором они добавлялись при настройке, можно в конверсиях последующих
     * атрибутов расчитьывать на наличие сконвертированных значений по предшествующим
     * атрибутам в поле {@link DestinationContext#destinationElement}.<br/><br/>
     *
     * Если включен режим копирования всех незатронутых атрибутов (методом
     * {@link DestinationContext#copyUntouched()}, незатронутые атрибуты обрабатываются
     * после тех, для которых конверсия задана явно. Порядок обработки нетронутых
     * атрибутов не гарантируется.
     */
    public class DestinationContext {
        private boolean completed = false;

        public final E2Element destinationElement;

        private boolean copyUntouched = false;
        private LinkedHashMap<String, AttributeConversion> conversions = new LinkedHashMap<>();

        private DestinationContext(E2Element destinationElement) {
            this.destinationElement = destinationElement;
        }

        public DestinationContext setChanged(boolean changed) {
            destinationElement.setChanged(changed);
            return this;
        }

        public DestinationContext setDeleted(boolean deleted) {
            destinationElement.setDeleted(deleted);
            return this;
        }


        /**
         * Включение флага копирования без изменения всех  атрибутов,
         * незатронутых конверсией.
         */
        public DestinationContext copyUntouched() {
            this.copyUntouched = true;
            return this;
        }

        /**
         * Позволяет отключить конверсию для перечисленных атрибутов.
         * @param attributeNames Массив атрибутов
         */
        public DestinationContext skip(String...attributeNames) {
            for (String attributeName: attributeNames) {
                conversions.put(attributeName, null);
            }
            return this;
        }

        public AttributeConversion take(String attributeName) {
            AttributeConversion conversion = new AttributeConversion();
            conversions.put(attributeName, conversion);
            return conversion;
        }

        /**
         * Запускает выполнение всех конверсий для данного элемента.
         */
        public void make() {
            if (!completed) {
                conversions.forEach((attributeName, conversion) -> {
                    if (conversion != null) {
                        sourceElement.attributes.get(attributeName).ifPresent(sourceAttribute
                                -> conversion.apply(sourceAttribute, destinationElement.attributes));
                    }
                });

                sourceElement.attributes.stream()
                        .filter(sourceAttribute -> !conversions.containsKey(sourceAttribute.attributeName()))
                        .forEach(sourceAttribute
                                -> COPY.apply(sourceAttribute, destinationElement.attributes));

                completed = true;
            } else {
                throw new E2Exception4Write("Conversions already completed for this destination element!");
            }
        }

    }

    public class AttributeConversion {
        private String destinationName;
        private String explicitEntity;
        private Function<E2Scalar, E2Scalar> conversion;

        private AttributeConversion() {}

        public AttributeConversion rename(String destinationName) {
            this.destinationName = destinationName;
            return this;
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
            return converter.input.referencedElement(value)
                    .map(converter::convertElement)
                    .map(converted -> explicitEntity != null ? converted.get(explicitEntity) : converted.get())
                    .orElseThrow(() -> new E2Exception4Write("Possibly, explicitEntity is wrong, or you need to provide it!"));
        }


        private String destinationName(E2Attribute sourceAttribute) {
            return destinationName != null ? destinationName : sourceAttribute.attributeName();
        }

        public void apply(E2Attribute sourceAttribute, E2Attributes destinationAttributes) {
            applyConversion(sourceAttribute.attributeValue())
                    .apply(destinationAttributes.add(destinationName(sourceAttribute)));
        }
    }
}