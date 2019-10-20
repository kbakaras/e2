package ru.kbakaras.e2.core.conversion.context;

import ru.kbakaras.e2.core.conversion.Conversion;
import ru.kbakaras.e2.core.conversion.Converter4Payload;
import ru.kbakaras.e2.core.conversion.Dereferencer;
import ru.kbakaras.e2.core.converted.Converted;
import ru.kbakaras.e2.message.E2Attribute;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Exception4Read;

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
     * @param conversion4Element
     */
    public E2Element addDestination(Conversion4Element conversion4Element) {
        return conversion4Element.make(this, this::addDestination);
    }

    /**
     * Метод запускает порождение элемента указанным продюсером. Элемент
     * добавляется в качестве целевого элемента, а также устанавливается в качестве
     * результата для данной конверсии. В большинстве стандартных случаев конверсии
     * вызываться должен именно этот метод.
     * @param conversion4Element
     */
    public E2Element addResult(Conversion4Element conversion4Element) {
        return conversion4Element.make(this, this::setResult);
    }

    /**
     * Принудительная установка существующей ссылки на элемент в качестве результата
     * данной конверсии. Используется в редких нестандартных случаях, когда результирующий
     * элемент уже сгенерирован где-то заранее и закэширован.<br/>
     * Указанный элемент при этом также добавляется и в набор целевых элементов данной конверсии.
     * Это нужно для того, чтобы его можно было обнаружить при поиске по стеку контекстов.
     * @param destinationElement Ссылка на элемент, устанавливаемая в качестве результата
     *                             данной конверсии.
     */
    public void setResult(E2Element destinationElement) {
        converted.put(destinationElement.asReference());
        addDestination(destinationElement);
    }

    /**
     * Принудительное добавление существующего элемента только в набор целевых (выходных) элементов
     * для данной конверсии. В качестве результата данной конверсии этот элемент не устанавливается.
     * @param destinationElement Элемент, добавляемый в набор целевых элементов данной конверсии.
     */
    public void addDestination(E2Element destinationElement) {
        destinationElements.add(destinationElement);
    }

    /**
     * В некоторых случаях, в зависимости от данных, нужно уметь игнорировать исходный элемент.
     * Данный метод позволяет установить пометку, что была предпринята попытка конвертировать
     * исходный элемент, но решено его игнорировать. Все реквизиты, которые ссылались на данный
     * исходный элемент будут пропущены (не попадут в результирующее сообщение).
     */
    public void ignoreResult() {
        converted.ignore();
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


    public Optional<E2Element> sourceDereference(String... attributeNames) {
        return new Dereferencer(converter.input, sourceElement)
                .dereference(attributeNames);
    }
    public E2Element sourceDereferenceMandatory(String... attributeNames) {
        return new Dereferencer(converter.input, sourceElement)
                .dereference(attributeNames)
                .orElseThrow(() -> new E2Exception4Read("Unable to deference mandatory attribute!"));
    }

}