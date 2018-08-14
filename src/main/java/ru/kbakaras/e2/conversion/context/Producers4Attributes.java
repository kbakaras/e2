package ru.kbakaras.e2.conversion.context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Producers4Attributes implements IProducers4Attributes {
    private boolean copyUntouched = false;
    private Set<String> skip = new HashSet<>();

    protected LinkedList<Producer> producers = new LinkedList<>();

    @Override
    public void make(ConversionContext4Element cce, ConversionContext4Producer ccp) {

    }

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
        return new Producer4AttributeSetup();
    }

    public class Producer4AttributeSetup {
        public Conversion4Attribute take(String sourceName) {
            Conversion4Attribute ca = new Conversion4Attribute();
            producers.add(ca);
            return ca;
        }

        public void produce(Producer4Attribute producer) {
            producers.add(producer);
        }

        public void value(String value) {
            producers.add(new ValueProducer());
        }
    }
}