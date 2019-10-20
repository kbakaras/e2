package ru.kbakaras.e2.core.conversion;

import ru.kbakaras.sugar.lazy.MapCache;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * При создании объект данного класса инициализируется картой, содержащей соответствие
 * имени исходной сущности и класса конверсии для одного заданного варианата:<br/>
 * {@code ТипИсходнойСистемы -> ТипРезультирующейСистемы}.<br/><br/>
 * Задача объекта, по мере необходимости выдавать инстанциированные объекты конверсий
 * в ответ на запрос по имени исходной сущности.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Conversions extends MapCache<String, Conversion> {

    public Conversions(Map<String, Class<? extends Conversion>> conversionMap) {
        this(conversionMap, null);
    }

    /**
     * Специальный вариант конструктора. Используется в тестах, позволяет задать
     * "поставщика" идентификаторов для использования в синтетических ключах. Это позволяет
     * получить повторяемые выходные сообщения за счёт <b>псевдо-</b>случайности поставляемых
     * идентификаторов.
     */
    public Conversions(Map<String, Class<? extends Conversion>> conversionMap, Supplier<UUID> uidSupplier) {

        super(inputName -> {
            Class<? extends Conversion> clazz = conversionMap != null ? conversionMap.get(inputName) : null;
            if (clazz != null) {
                try {

                    Conversion conversion = clazz.newInstance();

                    if (uidSupplier != null) {
                        conversion.setUidSupplier(uidSupplier);
                    }

                    return conversion;

                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            return new ConversionCopy(inputName);
        });

    }


}