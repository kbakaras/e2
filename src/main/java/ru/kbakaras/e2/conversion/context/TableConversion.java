package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Row;
import ru.kbakaras.e2.message.E2Table;

public class TableConversion extends TableProducer {
    private String sourceName;
    private String destinationName;

    private Producers4Attributes producers = new Producers4Attributes();

    public TableConversion(String sourceName, String destinationName) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
    }

    public IProducers4Attributes attributes() {
        return producers;
    }
    public Producers4Attributes.Producer4AttributeSetup attributes(String attributeName) {
        return producers.attribute(attributeName);
    }

    @Override
    public void make(ConversionContext4Producer ccp) {
        ccp.parent.sourceElement.table(sourceName).ifPresent(
                sourceTable -> {
                    E2Table destinationTable = ccp.parent.destinationElement.addTable(destinationName);
                    for (E2Row row: sourceTable) {
                        producers.make(
                                new ConversionContext4Producer(ccp.parent,
                                        row.attributes,
                                        destinationTable.addRow().attributes)
                        );
                    }
                }
        );
    }
}