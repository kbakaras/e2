package ru.kbakaras.e2.conversion.context;

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
}
