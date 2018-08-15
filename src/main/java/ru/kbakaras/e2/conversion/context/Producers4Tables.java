package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Table;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class Producers4Tables {
    private boolean copyUntouched = false;
    private Set<String> skip = new HashSet<>();
    private Producers producers;

    Producers4Tables(Producers producers) {
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

    public Producer4Table table(String tableName) {
        return new Producer4Table(tableName);
    }


    void makeAuto(ConversionContext4Producer ccp) {
        if (copyUntouched) {
            ccp.parent.sourceElement.tables().stream()
                    .map(E2Table::tableName)
                    .filter(sourceName -> !skip.contains(sourceName))
                    .forEach(sourceName -> new Conversion4Table(sourceName, sourceName)
                            .make(ccp));
        }
    }


    public class Producer4Table {
        private String destinationName;

        Producer4Table(String destinationName) {
            this.destinationName = destinationName;
        }

        public void produce(BiConsumer<ConversionContext4Element, Lazy<E2Table>> producer) {
            producers.add(new Producer() {
                @Override
                void make(ConversionContext4Producer ccp) {
                    producer.accept(ccp.parent, Lazy.of(() -> ccp.parent.destinationElement.addTable(destinationName)));
                }
            });
        }

        public Conversion4Table take(String sourceName) {
            Conversion4Table tc = new Conversion4Table(sourceName, destinationName);
            producers.add(tc);
            return tc;
        }
    }
}