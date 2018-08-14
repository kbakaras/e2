package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Producers4Tables extends Producers4Attributes implements IProducers4Tables {
    private boolean copyUntouchedTables = false;
    private Set<String> skipTables = new HashSet<>();

    @Override
    public IProducers4Attributes copyUntouchedTables() {
        this.copyUntouchedTables = true;
        return this;
    }

    @Override
    public IProducers4Attributes skipTables(String... attributeNames) {
        skipTables.addAll(Arrays.asList(attributeNames));
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

    @Override
    public void make(ConversionContext4Producer ccp) {
        if (copyUntouchedTables) {
            ccp.parent.sourceElement.tables().stream()
                    .map(E2Table::tableName)
                    .filter(sourceName -> !skipTables.contains(sourceName))
                    .forEach(sourceName -> new TableConversion(sourceName, sourceName)
                            .make(ccp));
        }
        super.make(ccp);
    }
}