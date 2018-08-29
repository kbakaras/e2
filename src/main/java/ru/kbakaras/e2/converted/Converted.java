package ru.kbakaras.e2.converted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kbakaras.e2.conversion.ConversionKind;
import ru.kbakaras.e2.message.E2AttributeValue;
import ru.kbakaras.e2.message.E2Element;
import ru.kbakaras.e2.message.E2Exception4Write;
import ru.kbakaras.e2.message.E2Reference;
import ru.kbakaras.e2.message.E2Scalar;

/**
 * Базовый класс dto-объектов, содержащих результаты конверсии.
 * Объекты используются для кэширования конвертированных элементов, позволяют
 * задать соответствие между ссылкой на входящий элемент и результатом
 * конверсии.
 * <br/>
 * Во все объекты, кроме Split, срабатывает только первое помещение результата,
 * все  последующие попытки игнорируются, выводится соответствующий лог. Объект
 * Split работает аналогично, но с учётом выходной сущности: принимается только
 * первый результат для каждой сущности.
 */
public abstract class Converted {
    protected static final Logger LOG = LoggerFactory.getLogger(Converted.class);

    public final E2Element source;
    public final ConversionKind kind;

    private boolean virgin = true;
    private boolean ignore = false;

    Converted(E2Element source, ConversionKind kind) {
        this.source = source;
        this.kind = kind;
    }

    protected boolean isVirgin() {
        return virgin;
    }

    final public void put(E2AttributeValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Converted can't be null!");
        } else if (value instanceof E2Reference) {
            put((E2Reference) value);
        } else if (value instanceof E2Scalar) {
            put((E2Scalar) value);
        } else {
            throw new IllegalArgumentException();
        }

        virgin = false;
    }

    public void ignore() {
        ignore = true;
        virgin = false;
    }

    protected void put(E2Reference reference) {
        throw new E2Exception4Write("Converted 'Reference' is not acceptable result for conversion of kind '" + kind + "'!");
    }
    protected void put(E2Scalar scalar) {
        throw new E2Exception4Write("Converted 'Scalar' is not acceptable result for conversion of kind '" + kind + "'!");
    }

    final public E2AttributeValue get() {
        return get(null);
    }
    final public E2AttributeValue get(String explicitEntity) {
        if (!virgin) {
            return getValue(explicitEntity);
        } else {
            throw new E2Exception4Write(
                    "Unable to get converted value! Conversion is not done yet for element:\n" +
                            "\t" + source.toString());
        }
    }

    abstract protected E2AttributeValue getValue(String destinationEntity);

    public static Converted create(E2Element source, ConversionKind kind) {
        switch (kind) {
            case Simple: return new ConvertedSimple(source, kind);
            case Choice: return new ConvertedChoice(source, kind);
            case Split:  return new ConvertedSplit(source, kind);
            case Merge:  return new ConvertedMerge(source, kind);
            case Scalar: return new ConvertedScalar(source, kind);

            default: throw new IllegalArgumentException("Conversion kind '" + kind + "' is not supported yet!");
        }
    }

    protected static void warnPutIgnored() {
        LOG.warn("Converted-object already holds result! All subsequent puts are ignored.");
    }

    public boolean notIgnored() {
        return !ignore;
    }
}