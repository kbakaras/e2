package ru.kbakaras.e2.core.conversion.context;

import ru.kbakaras.e2.message.E2Row;
import ru.kbakaras.e2.message.E2Table;

public class Conversion4Table extends Producer {
    private String sourceName;
    private String destinationName;

    private Producers producers = new Producers();
    public final Producers4Attributes attributes = new Producers4Attributes(producers);

    public Conversion4Table(String sourceName, String destinationName) {
        this.sourceName = sourceName;
        this.destinationName = destinationName;
    }

    public Producers4Attributes.Producer4Attribute attributes(String attributeName) {
        return attributes.attribute(attributeName);
    }

    @Override
    void make(ConversionContext4Producer ccp) {
        ConversionContext4Element cce = ccp.parent;

        cce.sourceElement.table(sourceName).ifPresent(
                sourceTable -> {
                    E2Table destinationTable = cce.destinationElement.addTable(destinationName);
                    for (E2Row row: sourceTable) {
                        ConversionContext4Producer ccp4table = new ConversionContext4Producer(cce,
                                row.attributes,
                                destinationTable.addRow().attributes);

                        attributes.makeAuto(ccp4table);
                        producers.make(ccp4table);
                    }
                }
        );
    }
}