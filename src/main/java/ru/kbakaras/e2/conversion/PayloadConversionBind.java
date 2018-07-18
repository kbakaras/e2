package ru.kbakaras.e2.conversion;

import ru.kbakaras.e2.model.SystemType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PayloadConversionBind {
    Class<? extends SystemType> sourceType();
    Class<? extends SystemType> destinationType();
    String sourceEntity();
}
