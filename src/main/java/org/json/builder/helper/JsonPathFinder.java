package org.json.builder.helper;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.builder.core.JsonBuilder;

import java.util.Iterator;

public class JsonPathFinder {

    public static String getJsonPath(JsonNode rootNode, JsonNode subNode) {
        StringBuilder jsonPath = new StringBuilder();
        if (findJsonPath(rootNode, subNode, jsonPath, "")) {
            return jsonPath.toString();
        }
        return null; // Return null if subNode is not found in rootNode
    }

    private static boolean findJsonPath(JsonNode currentNode, JsonNode targetNode, StringBuilder jsonPath, String currentPath) {
        if (currentNode.equals(targetNode)) {
            jsonPath.append(currentPath);
            return true;
        }

        if (currentNode.isObject()) {
            for (var entry : iterable(currentNode.fields())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                if (findJsonPath(entry.getValue(), targetNode, jsonPath, newPath)) {
                    return true;
                }
            }
        } else if (currentNode.isArray()) {
            for (int i = 0; i < currentNode.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                if (findJsonPath(currentNode.get(i), targetNode, jsonPath, newPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static <T> Iterable<T> iterable(Iterator<T> iterator) {
        return () -> iterator;
    }
}
