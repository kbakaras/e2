package ru.kbakaras.e2.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;
import ru.kbakaras.e2.message.E2Scalar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

    public class DestinationContext {
        public final E2Element destinationElement;

        private Set<String> accessed = new HashSet<>();

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


        public void copyAttributes(String...attributeNames) {
            for (String attributeName: attributeNames) {
                attribute(attributeName).copy();
            }
        }

        public void copyAllAttributes(Predicate<E2Attribute> filter) {
            sourceElement.attributes.list().stream()
                    .filter(filter)
                    .map(AttributeConversion::new)
                    .forEach(AttributeConversion::copy);
        }

        /**
         * Копирует в элемент назначения все реквизиты, кроме тех, к которым был зафиксирован
         * доступ через контекст. Фиксируется только непосредственный доступ через метод контекста
         * {@link DestinationContext#attribute(String)}, но не опосредованный,
         * через сам исходный элемент ({@link ConversionContext#sourceElement}).
         */
        public void copyNotAccessed() {
            copyAllAttributes(attr -> !accessed.contains(attr.attributeName()));
        }

        public AttributeConversion attribute(String attributeName) {
            accessed.add(attributeName);
            return new AttributeConversion(attributeName);
        }

        public boolean attributeBoolean(String attributeName) {
            accessed.add(attributeName);
            return sourceElement.attributes.getBoolean(attributeName);
        }

        public boolean attributeEquals(String attributeName, String attributeValue) {
            accessed.add(attributeName);
            return sourceElement.attributes.get(attributeName)
                    .map(attr -> attr.value().string().equals(attributeValue))
                    .orElse(false);
        }

        public class AttributeConversion {
            private Function<E2Scalar, E2Scalar> conversion;

            private String attributeName;
            private String destinationName;
            private String explicitEntity;

            private E2Attribute attribute;

            private AttributeConversion(String attributeName) {
                this.attributeName   = attributeName;
                this.destinationName = attributeName;
            }
            private AttributeConversion(E2Attribute attribute) {
                this.attribute       = attribute;
                this.attributeName   = attribute.attributeName();
                this.destinationName = this.attributeName;
            }

            public AttributeConversion convert(Function<E2Scalar, E2Scalar> conversion) {
                this.conversion = conversion;
                return this;
            }

            public AttributeConversion convertString(Function<String, String> conversion) {
                this.conversion = value -> new E2Scalar(conversion.apply(value.string()));
                return this;
            }

            public AttributeConversion explicitEntity(String explicitEntity) {
                this.explicitEntity = explicitEntity;
                return this;
            }

            public Optional<E2Attribute> copy() {
                return optional()
                        .map(E2Attribute::attributeValue)
                        .map(this::applyConversion)
                        .map(value -> value.apply(destinationElement.attributes.add(destinationName)));
            }

            public Optional<E2Attribute> copyTo(String destinationName) {
                this.destinationName = destinationName;
                return copy();
            }

            private E2AttributeValue applyConversion(E2AttributeValue value) {
                if (value instanceof E2Scalar) {
                    return conversion != null ? conversion.apply((E2Scalar) value) : value;
                } else if (value instanceof E2Reference) {
                    if (conversion != null) {
                        LOG.warn("Conversion is not applicable for reference-valued attributes! Conversion ignored.");
                    }
                    return converter.input.referencedElement((E2Reference) value)
                            .map(converter::convertElement)
                            .map(converted -> converted.get(explicitEntity))
                            .orElseThrow(() -> new E2Exception4Write("Possibly, explicitEntity is wrong, or you need to provide it!"));
                } else {
                    throw new E2Exception4Write("Unknown attribute value type!");
                }
            }

            public Optional<E2Attribute> optional() {
                return attribute != null ? Optional.of(attribute) : sourceElement.attributes.get(attributeName);
            }
        }
    }
}