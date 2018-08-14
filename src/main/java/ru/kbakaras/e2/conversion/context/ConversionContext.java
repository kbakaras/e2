package ru.kbakaras.e2.conversion.context;

import ru.kbakaras.e2.conversion.Conversion;
import ru.kbakaras.e2.conversion.Converter4Payload;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Объект-контекст, передаваемый в специализированную конверсию. На основании
 * таких объектов и по мере запуска конверсий для атрибутов-ссылок формируется
 * стек контекстов конвертации.
 */
public class ConversionContext {
    public final Converter4Payload converter;
    public final E2Element sourceElement;
    public final Converted converted;

    public final Conversion conversion;

    /**
     * Список всех целевых элементов, порождённых в рамках данного
     * контекста. Не все они обязательно попадут в результаты конверсии,
     * но в выходном сообщении окажутся все. А также они будут доступны
     * для поиска в стеке контекстов методом {@link #findDestinationContext(String, String)}.
     */
    private List<E2Element> destinationElements = new ArrayList<>();


    public ConversionContext(Converter4Payload converter, Conversion conversion, E2Element sourceElement, Converted converted) {
        this.converter     = converter;
        this.sourceElement = sourceElement;
        this.converted     = converted;
        this.conversion    = conversion;
    }


    /**
     * Добавление целевого элемента, порождённого данным продюсером.
     * Метод запускает порождение элемента указанным продюсером. Элемент
     * добавляется в качестве целевого элемента, но не добавляется в кэш
     * результатов. Метод нужен в редких случаях, когда из одного исходного
     * элемента нужно породить не только результирующий элемент, но и побочный.
     * @param elementProducer
     */
    public void addDestination(ElementProducer elementProducer) {
        destinationElements.add(elementProducer.make(this));
    }
    public void addResult(ElementProducer elementProducer) {
        E2Element element = elementProducer.make(this);
        destinationElements.add(element);
        setResult(element.asReference());
    }
    public void setResult(E2Reference destinationReference) {
        converted.put(destinationReference);
    }


    /**
     * Поиск в стеке контекстов атрибута <i>attributeName</i> в элементе сущности <i>sourceEntity</i>.
     * @param entityName Имя искомой сущности
     * @param attributeName Имя искомого реквизита
     * @return Первый найденный реквизит
     */
    public E2Attribute findDestinationContext(String entityName, String attributeName) {
        for (ConversionContext context: converter.contextStack()) {
            for (E2Element destinationElement: context.destinationElements) {
                if (destinationElement.entityName().equals(entityName)) {
                    Optional<E2Attribute> attribute = destinationElement.attributes.get(attributeName);
                    if (attribute.isPresent()) {
                        return attribute.get();
                    }
                }
            }
        }
        return null;
    }

}