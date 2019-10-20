package ru.kbakaras.e2.core.conversion.context;

import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeUse;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Scalar;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Producers4Attributes {
    private boolean copyUntouched = false;
    private Set<String> skip = new HashSet<>();
    private Producers producers;

    Producers4Attributes(Producers producers) {
        this.producers = producers;
    }

    public Producers4Attributes copyUntouched() {
        this.copyUntouched = true;
        return this;
    }

    public Producers4Attributes skip(String... attributeNames) {
        skip.addAll(Arrays.asList(attributeNames));
        return this;
    }

    public void take(String... sourceNames) {
        for (String sourceName: sourceNames) {
            new Producer4Attribute(sourceName).take(sourceName);
        }
    }

    public Producer4Attribute attribute(String attributeName) {
        return new Producer4Attribute(attributeName);
    }

    public Conversion4Attribute take(String sourceName) {
        return new Producer4Attribute(sourceName).take(sourceName);
    }

    public Producers4Attributes produce(Consumer<ConversionContext4Producer> producer) {
        producers.add(new Producer() {
            @Override
            void make(ConversionContext4Producer ccp) {
                producer.accept(ccp);
            }
        });
        return this;
    }


    void makeAuto(ConversionContext4Producer ccp) {
        if (copyUntouched) {
            ccp.sourceAttributes.stream()
                    .map(E2Attribute::attributeName)
                    .filter(sourceName -> !skip.contains(sourceName))
                    .forEach(sourceName ->
                        new Conversion4Attribute(
                                sourceName, sourceName,
                                ccp_ -> ccp_.destinationAttributes.add(sourceName)
                        ).make(ccp)
                    );
        }
    }


    public class Producer4Attribute {
        private String destinationName;
        private E2Attribute destinationAttribute;

        private boolean id = false;
        private E2AttributeUse use;
        private E2AttributeValue defaultValue;

        Producer4Attribute(String destinationName) {
            this.destinationName = destinationName;
        }

        /**
         * Метод позволяет задать, будет ли создаваемый атрибут иметь признак, что он является
         * идентифицирующим. Обращение к данному методу не приводит к обязательному созданию
         * реквизита, но если реквизит будет создан, признак ему будет назначен.
         */
        public Producer4Attribute id(boolean id) {
            this.id = id;
            return this;
        }

        public Producer4Attribute use(E2AttributeUse use) {
            this.use = use;
            return this;
        }

        public Producer4Attribute defaultValue(String value) {
            this.defaultValue = new E2Scalar(value);
            return this;
        }


        /**
         * Создаёт стандартную конверсию для данного выходного атрибута, указав имя входного
         * атрибута. Входной атриюут автоматически помещается в набор skip и не будет использоваться
         * при выполнения автоматической конверсии "нетронутых" атрибутов.
         * @param sourceName Имя исходного атрибута
         */
        public Conversion4Attribute take(String sourceName) {
            Conversion4Attribute ca = new Conversion4Attribute(sourceName, destinationName, this::createAttribute);
            addProducer(ca);
            skip.add(sourceName);
            return ca;
        }

        /**
         * Лёгкий вариант задать кастомный продьюсер для атрибута. Второй параметр
         * биконсьюмера - ленивый новый атрибут с уже заданным именем. Атрибут будет
         * создан только в том случае, если к нему будет обращение, поэтому в консьюмере
         * остаётся вариант условного создания атрибута.
         * @param producer Консьюмер, который будет создавать атрибут
         */
        public void produce(BiConsumer<ConversionContext4Producer, Lazy<E2Attribute>> producer) {
            addProducer(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    producer.accept(ccp, Lazy.of(() -> createAttribute(ccp)));
                }
            });
        }

        public void value(String value) {
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    createAttribute(ccp).setValue(value);
                }
            });
        }

        public void value(E2AttributeValue value) {
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    value.apply(createAttribute(ccp));
                }
            });
        }

        /**
         * Ссылка на метод передаётся в функции, производящие атрибут. Метод создаёт атрибут
         * с заданным именем и выполняет необходимую базовую настройку. Если метод будет вызван,
         * он в том числе сохранит ссылку на созданный атрибут в данном классе.
         */
        private E2Attribute createAttribute(ConversionContext4Producer ccp) {
            this.destinationAttribute = ccp.destinationAttributes.add(destinationName)
                    .setId(id).setUse(use);
            return destinationAttribute;
        }

        private void addProducer(Producer producer) {
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    producer.make(ccp);

                    if (destinationAttribute == null && defaultValue != null) {
                        defaultValue.apply(createAttribute(ccp));
                    }
                }
            });
        }
    }
}