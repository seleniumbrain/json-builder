package org.json.builder.core;

import lombok.Getter;

@Getter
public enum NodeType {
    NUMBER("Number"),
    INT("Int"),
    INTEGER("Integer"),
    LONG("Long"),
    FLOAT("Float"),
    DECIMAL("Decimal"),
    DOUBLE("Double"),
    TEXT("Text"),
    STRING("String"),
    BOOLEAN("Boolean"),
    NULL("Null"),
    BLANK("Blank"),
    EMPTY("Empty"),
    EMPTYOBJECT("ObjectNode"),
    EMPTYARRAY("ArrayNode"),
    OBJECTNODE("JsonObject"),
    ARRAYNODE("JsonArray"),
    SKIP("Skip"),
    IGNORE("Ignore");

    private final String type;

    NodeType(String type) {
        this.type = type;
    }

    public static NodeType fromString(String text) {
        for (NodeType b : NodeType.values()) {
            if (b.type.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return STRING;
    }

}