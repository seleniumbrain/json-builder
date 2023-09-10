package org.json.builder.core;

import lombok.Getter;

@Getter
public enum NodeValueType {
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
        OBJECTNODE("ObjectNode"),
        ARRAYNODE("ArrayNode"),
        OBJECT_STYLE_JSON("JsonObject"),
        ARRAY_STYLE_JSON("JsonArray"),
        SKIP("Skip"),
        IGNORE("Ignore");

        private final String type;

        NodeValueType(String type) {
            this.type = type;
        }

    }