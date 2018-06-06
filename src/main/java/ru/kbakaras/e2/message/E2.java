package ru.kbakaras.e2.message;

import ru.kbakaras.sugar.utils.MapBuilder;

import java.util.Map;

/**
 * Создано: kbakaras, в день: 04.03.2018.
 */
public class E2 {
    public static final String NS = "http://agr.glance.ru/e2";

    public static final Map<String, String> E2MAP = MapBuilder.map("e2", E2.NS).getUnmodifiable();

    public static final String ENTITY         = "entity";
    public static final String ENTITY_NAME    = "entityName";
    public static final String ENTITY_VERSION = "entityVersion";
    public static final String ELEMENT        = "element";
    public static final String ELEMENT_UID    = "elementUid";
    public static final String CHANGED        = "changed";
    public static final String ATTRIBUTE      = "attribute";
    public static final String ATTRIBUTE_NAME = "attributeName";
    public static final String REFERENCE      = "reference";
    public static final String VALUE          = "value";
    public static final String CONDITION      = "condition";
    public static final String ERROR          = "error";
    public static final String TABLE          = "table";
    public static final String TABLE_NAME     = "tableName";
    public static final String ROW            = "row";

    public static final String URIB           = "urib";

    public static final String CONTEXT        = "context";
    public static final String SYSTEM         = "system";
    public static final String SYSTEM_UID     = "systemUid";
    public static final String SYSTEM_NAME    = "systemName";
}