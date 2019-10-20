package ru.kbakaras.e2.core.conversion;

import ru.kbakaras.e2.message.E2EntityRequest;
import ru.kbakaras.e2.message.E2Filter;
import ru.kbakaras.e2.message.E2Reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>Элементы данного класса лениво инстанциируются для каждой конверсии.
 * Они содержат правила для конвертации фильтров, которые могут быть
 * указаны в зпросах entityRequest для сущности, связанной с данной
 * конверсией.</p>
 * <p>Каждое правило соответствует имени входящего реквизита. Суть правила
 * может заключаться в игнорировании фильтра, в переименовании реквизита,
 * в преобразовании значения фильтрации и операции. Игнорирование фильтра означает,
 * что он не попадёт в результирующий запрос.</p>
 * <p>Если в правилах не содержится ни одного обработчика для фильтра
 * по какому-либо реквизиту, то, по умолчанию, реквизит просто копируется,
 * то есть, в результирующем запросе он не будет переименован, а значение
 * не подвергнется никакому преобразованию, если это простое значение, но,
 * если это ссылка, то имя сущности может быть переименовани в соответствии
 * с нужной конверсией.</p>
 */
public class ConversionRules4Filters {
    private Map<String, Rule> rules = new HashMap<>();

    void apply(E2Filter sourceFilter, E2EntityRequest destinationRequest, Converter4Request converter) {
        Rule rule = Optional.ofNullable(rules.get(sourceFilter.attributeName())).orElse(duplicateRule);

        if (!rule.ignore) {
            if (rule.ignorePredicate == null ||  !rule.ignorePredicate.test(sourceFilter)) {
                E2Filter destinationFilter = destinationRequest.addFilter(
                        Optional.ofNullable(rule.destinationName)
                                .orElse(sourceFilter.attributeName()));

                destinationFilter.setCondition(sourceFilter.condition());

                if (!sourceFilter.isReference()) {
                    sourceFilter.value().apply(destinationFilter);

                } else {
                    E2Reference sourceReference = sourceFilter.reference();

                    Optional<E2Filter> convertedFilter = converter.input.context()
                            .flatMap(payload -> payload.referencedElement(sourceReference))
                            .map(converter.context.get()::convertElement)
                            .map(converted -> converted.get().apply(destinationFilter));

                    if (!convertedFilter.isPresent()) {
                        sourceReference.apply(destinationFilter);
                    }
                }

                if (rule.conversion != null) {
                    rule.conversion.accept(destinationFilter);
                }
            }
        }
    }

    public RuleSetup add(String attributeName) {
        Rule rule = new Rule();
        rules.put(attributeName, rule);
        return new RuleSetup(rule);
    }

    private static class Rule {
        private boolean             ignore;
        private String              destinationName;
        private Predicate<E2Filter> ignorePredicate;
        private Consumer<E2Filter>  conversion;
    }

    public static class RuleSetup {
        private Rule rule;

        private RuleSetup(Rule rule) {
            this.rule = rule;
        }

        public void ignore() {
            rule.ignore = true;
        }

        public RuleSetup ignore(Predicate<E2Filter> ignorePredicate) {
            rule.ignorePredicate = ignorePredicate;
            return this;
        }

        public RuleSetup rename(String attributeName) {
            rule.destinationName = attributeName;
            return this;
        }

        public RuleSetup convert(Consumer<E2Filter> conversion) {
            rule.conversion = conversion;
            return this;
        }

        public RuleSetup convertString(Function<String, String> conversion) {
            rule.conversion = filter -> filter.setValue(conversion.apply(filter.value().string()));
            return this;
        }
    }

    private static final Rule duplicateRule = new Rule();
}