package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.conversion.context.ConversionContext;
import ru.kbakaras.e2.converted.Converted;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Payload;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

/**
 * Класс является диспетчером для конверсий. Ключевый моменты:
 * <li>Запускает конверсию входного элемента.</li>
 * <li>Определяет конверсию, подходящую для входного элемента.</li>
 * <li>Содержит стек контекстов выполняемых конверсий.</li>
 * <li>Содержит кэш конвертированных элементов.</li>
 * <li>Содержит входное и выходное сообщение.</li>
 * <li>На каждое выходное сообщение нужен один объект-конвертор.</li>
 */
public class Converter4Payload {
    public final E2Payload input;
    public final E2Payload output;

    private Conversions conversions;
    private Stack<ConversionContext> contextStack = new Stack<>();

    private ConvertedCache convertedCache = new ConvertedCache();

    public Converter4Payload(E2Payload input, E2Payload output, Map<String, Class<? extends Conversion>> conversionMap) {
        this.input = input;
        this.output = output;
        this.conversions = new Conversions(conversionMap);
    }
    public Converter4Payload(E2Payload input, E2Payload output, Conversions conversions) {
        this.input = input;
        this.output = output;
        this.conversions = conversions;
    }

    public Converted convertElement(E2Element element) {
        if (!convertedCache.isConverting(element)) {
            Conversion conversion = conversions.get(element.entityName());
            convertedCache.init(element, conversion.kind);

            ConversionContext conversionContext = new ConversionContext(
                    this, conversion,
                    element, convertedCache.get(element)
            );

            contextStack.push(conversionContext);
            conversion.convertElement(conversionContext);
            contextStack.pop();
        }

        return convertedCache.get(element);
    }

    public Iterable<ConversionContext> contextStack() {
        return () -> new Iterator<ConversionContext>() {
            private ListIterator<ConversionContext> iterator = contextStack.listIterator(contextStack.size());

            @Override
            public boolean hasNext() {
                return iterator.hasPrevious();
            }

            @Override
            public ConversionContext next() {
                return iterator.previous();
            }
        };
    }

    public void convertChanged() {
        input.entities().forEach(
                entity -> entity.elementsChanged().forEach(this::convertElement));
    }
}