package ru.kbakaras.e2.message;

import org.dom4j.Element;

@SuppressWarnings("WeakerAccess")
public class E2SystemError {

    protected Element xml;

    public final E2SystemResponse parent;


    E2SystemError(Element xml, E2SystemResponse parent) {
        this.xml = xml;
        this.parent = parent;
    }

    E2SystemError(E2SystemResponse parent) {
        this.parent = parent;
        this.xml = parent.xml.addElement(E2.ERROR);
    }


    public E2SystemError setText(String text) {
        xml.setText(text);
        return this;
    }


    public String text() {
        return xml.getText();
    }

}