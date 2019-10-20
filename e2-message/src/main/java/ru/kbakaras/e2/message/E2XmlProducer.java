package ru.kbakaras.e2.message;

import org.dom4j.Element;

/**
 * Общий интерфейс для всех классов, которые могут возвращать xml в качестве своего содержимого.
 */
public interface E2XmlProducer {
    Element xml();
}
