package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2AttributeValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Producers4Attributes implements IProducers4Attributes {
    private boolean copyUntouched = false;
    private Set<String> skip = new HashSet<>();

    protected LinkedList<Producer> producers = new LinkedList<>();

    @Override
    public IProducers4Attributes copyUntouched() {
        this.copyUntouched = true;
        return this;
    }

    @Override
    public IProducers4Attributes skip(String... attributeNames) {
        skip.addAll(Arrays.asList(attributeNames));
        return this;
    }

    @Override
    public Producer4AttributeSetup attribute(String attributeName) {
        return new Producer4AttributeSetup(attributeName);
    }

    @Override
    public AttributeConversion take(String sourceName) {
        return new Producer4AttributeSetup(sourceName).take(sourceName);
    }

    @Override
    public void take(String... sourceNames) {
        for (String sourceName: sourceNames) {
            new Producer4AttributeSetup(sourceName).take(sourceName);
        }
    }

    public class Producer4AttributeSetup {
        private String destinationName;

        public Producer4AttributeSetup(String destinationName) {
            this.destinationName = destinationName;
        }

        public AttributeConversion take(String sourceName) {
            AttributeConversion ca = new AttributeConversion(sourceName, destinationName);
            producers.add(ca);
            return ca;
        }

        public void produce(AttributeProducer producer) {
            producers.add(producer);
        }

        public void value(String value) {
            producers.add(new AttributeProducer() {
                @Override
                public void make(ConversionContext4Producer ccp) {
                    ccp.destinationAttributes.add(destinationName).setValue(value);
                }
            });
        }

        public void value(E2AttributeValue value) {
            producers.add(new AttributeProducer() {
                @Override
                public void make(ConversionContext4Producer ccp) {
                    value.apply(ccp.destinationAttributes.add(destinationName));
                }
            });
        }
    }

    @Override
    public void make(ConversionContext4Producer ccp) {
        if (copyUntouched) {
            ccp.sourceAttributes.stream()
                    .map(E2Attribute::attributeName)
                    .filter(sourceName -> !skip.contains(sourceName))
                    .forEach(sourceName -> new AttributeConversion(sourceName, sourceName)
                            .make(ccp));
        }

        producers.forEach(producer -> producer.make(ccp));
    }
}