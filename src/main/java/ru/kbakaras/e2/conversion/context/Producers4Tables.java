package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Producers4Tables {
    private boolean copyUntouched = false;
    private Set<String> skip = new HashSet<>();
    private Producers producers;

    public Producers4Tables(Producers producers) {
        this.producers = producers;
    }

    public Producers4Tables copyUntouched() {
        this.copyUntouched = true;
        return this;
    }

    public Producers4Tables skip(String... attributeNames) {
        skip.addAll(Arrays.asList(attributeNames));
        return this;
    }

    public Producer4TableSetup table(String tableName) {
        return new Producer4TableSetup(tableName);
    }

    public class Producer4TableSetup {
        private String destinationName;

        public Producer4TableSetup(String destinationName) {
            this.destinationName = destinationName;
        }

        public void produce(TableProducer producer) {
            producers.add(producer);
        }

        public TableConversion take(String sourceName) {
            TableConversion tc = new TableConversion(sourceName, destinationName);
            producers.add(tc);
            return tc;
        }
    }

    void makeAuto(ConversionContext4Producer ccp) {
        if (copyUntouched) {
            ccp.parent.sourceElement.tables().stream()
                    .map(E2Table::tableName)
                    .filter(sourceName -> !skip.contains(sourceName))
                    .forEach(sourceName -> new TableConversion(sourceName, sourceName)
                            .make(ccp));
        }
    }
}