package ru.kbakaras.e2.core;

public abstract class RouteConfigurer {

    public abstract void setupRoutes(RouteConfigurer4Update update, RouteConfigurer4Request request);


    public static final String CONFIGURER_CLASS   = "E2-Configurer-Class";
    public static final String CONVERSION_PACKAGE = "E2-Conversion-Package";

}