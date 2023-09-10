package org.json.builder.core;

public class BuilderFactory {

    public static JsonBuilder getBuilder(String name) {
        if(name == null) return null;

        if(name.equalsIgnoreCase("JsonObjectBuilder"))
            return new JsonObjectBuilder();

        else if(name.equalsIgnoreCase("JsonArrayBuilder"))
            return new JsonArrayBuilder();

        else
            return null;
    }
}
