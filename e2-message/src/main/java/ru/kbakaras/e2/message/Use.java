package ru.kbakaras.e2.message;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.tree.DefaultNamespace;

/**
 * Создано: kbakaras, в день: 04.03.2018.
 */
public class Use {
    public static Element createRoot(String name, String nsUri, String nsPrefix) {
        return DocumentHelper
                .createDocument(DocumentHelper.createElement(
                        new QName(name, new DefaultNamespace(nsPrefix, nsUri))
                )).getRootElement();
    }
    public static Element createRoot(String name, String nsUri) {
        return DocumentHelper
                .createDocument(DocumentHelper.createElement(
                        new QName(name, new DefaultNamespace(null, nsUri))
                )).getRootElement();
    }

    public static Element parse4Root(String xmlStr) {
        try {
            return DocumentHelper.parseText(xmlStr).getRootElement();
        } catch (DocumentException e) {
            throw new IllegalArgumentException(e);
        }
    }
}