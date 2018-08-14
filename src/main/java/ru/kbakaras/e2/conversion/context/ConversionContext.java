package ru.kbakaras.e2.conversion.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private Conversion4Attribute COPY = new Conversion4Attribute(this);


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

        public ElementSetup ok() {
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

            return new ElementSetup(element)
                    .setChanged(sourceElement.isChanged())
                    .setDeleted(sourceElement.isDeleted());
        }
    }

    /**
     * Класс используется для настройки конверсий для атрибутов элемента.
     * Конверсии применяются последовательно (в порядке добавления) в момент
     * вызова метода {@link ElementSetup#make()}. В момент добавления
     * конверсии просто формируется отсортированный map.<br/><br/>
     *
     * Благодаря тому, что при выполнении конверсий соблюдается тот порядок,
     * в котором они добавлялись при настройке, можно в конверсиях последующих
     * атрибутов расчитывать на наличие сконвертированных значений по предшествующим
     * атрибутам в поле {@link ElementSetup#destinationElement}.<br/><br/>
     *
     * Если включен режим копирования всех незатронутых атрибутов (методом
     * {@link ElementSetup#copyUntouched()}, незатронутые атрибуты обрабатываются
     * до обработки тех, для которых конверсия задана явно. Порядок обработки нетронутых
     * атрибутов не гарантируется.
     */
    public class ElementSetup {
        private boolean completed = false;

        public final E2Element destinationElement;
        public final AttributesSetup attributesSetup;

        private Producers4Tables producers4Tables = new Producers4Tables();

        private boolean copyUntouched = false;
        private Set<String> skip = new HashSet<>();
        private LinkedHashMap<String, AttributeProducer> producers = new LinkedHashMap<>();

        private ElementSetup(E2Element destinationElement) {
            this.destinationElement = destinationElement;
            this.attributesSetup = new AttributesSetup();
        }

        public ElementSetup setChanged(boolean changed) {
            destinationElement.setChanged(changed);
            return this;
        }

        public ElementSetup setDeleted(boolean deleted) {
            destinationElement.setDeleted(deleted);
            return this;
        }


        public ElementSetup copyUntouched() {
            this.producers4Tables.copyUntouched();
            return this;
        }

        public ElementSetup skip(String... attributeNames) {
            this.producers4Tables.skip(attributeNames);
            return this;
        }


        public void attribute(String attributeName) {
            producers4Tables.attribute(attributeName);
        }

        public class TableConversionSetup {
            private String destinationName;

            public TableConversionSetup(String destinationName) {
                this.destinationName = destinationName;
            }
        }

        public void make(ConversionContext4Element cce) {
            producers4Tables.make(cce, new ConversionContext4Producer(
                    cce.sourceElement.attributes,
                    cce.destinationElement.attributes));
        }
    }

    private static class AttributeProducer {
        final Supplier<E2AttributeValue> supplier;
        final String                     sourceName;
        final String                     destinationName;
        final Conversion4Attribute ac;

        private AttributeProducer(String destinationName, String sourceName, Conversion4Attribute ac) {
            this.destinationName = destinationName;
            this.sourceName = sourceName;
            this.ac = ac;
            this.supplier   = null;
        }
        private AttributeProducer(String destinationName, Supplier<E2AttributeValue> supplier) {
            this.destinationName = destinationName;
            this.supplier   = supplier;
            this.sourceName = null;
            this.ac = null;
        }
    }

    public class TableSetup {
        private String sourceName;
        private String destinationName;


        public void make(ConversionContext context, E2Element destinationElement) {
            context.sourceElement.table(sourceName).ifPresent(source -> {
                E2Table destination = destinationElement.addTable(destinationName);
                for (E2Row row: source) {

                }
            });
        }
    }

    public class ProducersSetup {
        private boolean copyUntouched       = false;
        private boolean copyUntouchedTables = false;
        private Set<String> skip            = new HashSet<>();
        private Set<String> skipTables      = new HashSet<>();

        private LinkedList<Producer> producers = new LinkedList<>();

        /**
         * Включение флага копирования без изменения всех  атрибутов,
         * незатронутых конверсией.
         */
        public ProducersSetup copyUntouched() {
            this.copyUntouched = true;
            return this;
        }

        public ProducersSetup copyUntouchedTables() {
            this.copyUntouchedTables = true;
            return this;
        }

        /**
         * Позволяет для перечисленных атрибутов отключить автоматическую конверсию
         * (конверсия, выполняемая по флагу, включаемому методом {@link #copyUntouched()}).<br/>
         * Если конверсия для атрибута была ранее задана в явном виде, вызов данного метода
         * её не отменит.
         * @param attributeNames Массив атрибутов
         */
        public ProducersSetup skip(String...attributeNames) {
            skip.addAll(Arrays.asList(attributeNames));
            return this;
        }
        /**
         * Позволяет для перечисленных атрибутов отключить автоматическую конверсию
         * (конверсия, выполняемая по флагу, включаемому методом {@link #copyUntouched()}).<br/>
         * Если конверсия для атрибута была ранее задана в явном виде, вызов данного метода
         * её не отменит.
         * @param tableNames Массив атрибутов
         */
        public ProducersSetup skipTables(String...tableNames) {
            skipTables.addAll(Arrays.asList(tableNames));
            return this;
        }

        public ProducersSetup copy(String...attributeNames) {
            for (String attributeName: attributeNames) {
                takeFor(attributeName, attributeName);
            }
            return this;
        }
        public ProducersSetup copyTables(String...tableNames) {
            for (String attributeName: tableNames) {
                takeFor(attributeName, attributeName);
            }
            return this;
        }

        public Conversion4Attribute take(String attributeName) {
            return takeFor(attributeName, attributeName);
        }

        public AttributeSetup to(String destinationName) {
            return new AttributeSetup(destinationName);
        }


        private Conversion4Attribute takeFor(String sourceName, String destinationName) {
            Conversion4Attribute ac = new Conversion4Attribute(ConversionContext.this);
            skip.add(sourceName);
            attributeProducers.add(
                    new AttributeProducer(destinationName, sourceName, ac));

            return ac;
        }

        public class AttributeSetup {
            private String destinationName;

            private AttributeSetup(String destinationName) {
                this.destinationName = destinationName;
            }

            public Conversion4Attribute take(String sourceName) {
                return takeFor(sourceName, destinationName);
            }

            public AttributesSetup value(String value) {
                attributeProducers.add(
                        new AttributeProducer(destinationName, () -> new E2Scalar(value)));
                return AttributesSetup.this;
            }
        }


        public void make(ConversionContext4Element cce, ConversionContext4Producer ccp) {
            if (copyUntouched) {
                ccp.sourceAttributes.stream()
                        .filter(sourceAttribute -> !skip.contains(sourceAttribute.attributeName()))
                        .forEach(sourceAttribute -> COPY.apply(
                                sourceAttribute,
                                ccp.destinationAttributes.add(sourceAttribute.attributeName())));
            }

            if (copyUntouchedTables) {

            }

            attributeProducers.forEach(producer -> {
                if (producer.ac != null) {
                    sourceAttributes.get(producer.sourceName).ifPresent(
                            sourceAttribute -> producer.ac.apply(
                                    sourceAttribute,
                                    destinationAttributes.add(producer.destinationName)));
                } else {
                    producer.supplier.get().apply(
                            destinationAttributes.add(producer.destinationName)
                    );
                }
            });

//            if (!completed) {
//                completed = true;
//            } else {
//                throw new E2Exception4Write("Conversions already completed for this destination element!");
//            }
        }
    }

}