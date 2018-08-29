package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeValue;
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
                    .forEach(sourceName -> new Conversion4Attribute(sourceName, sourceName)
                            .make(ccp));
        }
    }


    public class Producer4Attribute {
        private String destinationName;

        Producer4Attribute(String destinationName) {
            this.destinationName = destinationName;
        }

        public Conversion4Attribute take(String sourceName) {
            Conversion4Attribute ca = new Conversion4Attribute(sourceName, destinationName);
            producers.add(ca);
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
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    producer.accept(ccp, Lazy.of(() -> ccp.destinationAttributes.add(destinationName)));
                }
            });
        }

        public void value(String value) {
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    ccp.destinationAttributes.add(destinationName).setValue(value);
                }
            });
        }

        public void value(E2AttributeValue value) {
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    value.apply(ccp.destinationAttributes.add(destinationName));
                }
            });
        }
    }
}