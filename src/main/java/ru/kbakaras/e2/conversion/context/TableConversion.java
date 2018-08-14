package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.message.E2Row;
import ru.kbakaras.e2.message.E2Table;

public class TableConversion extends TableProducer {
    private String sourceName;
    private String destinationName;

    private Producers producers = new Producers();
    public final Producers4Attributes attributes = new Producers4Attributes(producers);

    public TableConversion(String sourceName, String destinationName) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
    }

    public Producers4Attributes.Producer4AttributeSetup attributes(String attributeName) {
        return attributes.attribute(attributeName);
    }

    @Override
    public void make(ConversionContext4Producer ccp) {
        ccp.parent.sourceElement.table(sourceName).ifPresent(
                sourceTable -> {
                    E2Table destinationTable = ccp.parent.destinationElement.addTable(destinationName);
                    for (E2Row row: sourceTable) {
                        ConversionContext4Producer ccp2 = new ConversionContext4Producer(ccp.parent,
                                row.attributes,
                                destinationTable.addRow().attributes);

                        attributes.makeAuto(ccp2);
                        producers.make(ccp2);
                    }
                }
        );
    }
}