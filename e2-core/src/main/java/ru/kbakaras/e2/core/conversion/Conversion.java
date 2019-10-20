package ru.kbakaras.e2.core.conversion;

import ru.kbakaras.e2.core.conversion.context.ConversionContext;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.sugar.lazy.Lazy;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Классы конверсий должны наследоваться от данного класса.<br/><br/>
 *
 * Связывание класса конверсии с типами систем и названием конвертируемой сущности
 * задаётся при помощи аннотации {@link PayloadConversionBind}.<br/><br/>
 *
 * Экземпляр класса конверсии (конкретной) может содержать состояние. Жизненный цикл
 * экземпляра ограничен одним выходным сообщением. То есть, можно расчитывать, что
 * состояние, сохраняемое в экземпляре, будет иметь отношение только к одному
 * выходному сообщению.
 */
public abstract class Conversion {

    public final Lazy<ConversionRules4Filters> conversionRules4Filters;

    public final String[] destinationEntities;
    public final ConversionKind kind;


    private Supplier<UUID> uidSupplier = Converter4Payload.randomUidSupplier;


    public Conversion(ConversionKind kind, String...destinationEntities) {
        this.destinationEntities = destinationEntities;
        this.kind = kind;

        this.conversionRules4Filters = Lazy.of(() -> {
            ConversionRules4Filters rules = new ConversionRules4Filters();
            initConversionRules4Filters(rules);
            return rules;
        });
    }

    public Conversion(String destinationEntity) {
        this(ConversionKind.Simple, new String[] {destinationEntity});
    }


    void setUidSupplier(Supplier<UUID> uidSupplier) {
        this.uidSupplier = uidSupplier;
    }

    protected String randomUid() {
        return uidSupplier.get().toString();
    }


    public String getDefaultDestinationEntity() {
        if (kind == ConversionKind.Merge || kind == ConversionKind.Simple) {
            return destinationEntities[0];
        } else {
            throw new E2Exception4Write(
                    "Can't determine defaule destination entity for conversion " +
                            this.getClass().getSimpleName() + " of kind " + kind + "!");
        }
    }

    abstract public void convertElement(ConversionContext context);

    protected void initConversionRules4Filters(ConversionRules4Filters rules) {}

    static protected Function<String, String> mappingFunction(Map<String, String> mapping, String defaultValue) {
        return input -> {
            String output = mapping.get(input);
            if (output == null) output = defaultValue;
            return output;
        };
    }

}