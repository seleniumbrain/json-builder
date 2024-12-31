/**
 *
 */
package org.json.builder.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author rajkumarrajamani
 */
public interface JsonBuilder {

    ObjectMapper MAPPER = new ObjectMapper();

    static JsonBuilder objectBuilder() {
        return new JsonObjectBuilder();
    }

    static JsonBuilder arrayBuilder() {
        return new JsonArrayBuilder();
    }

    JsonBuilder fromJsonFile(String jsonFileName);

    JsonBuilder fromJsonFile(File jsonFile);

    JsonBuilder fromJsonString(String json);

    JsonBuilder fromEmptyNode();

    JsonBuilder update(String jsonNodePath, Object value, NodeType dataTypeOfValue);

    JsonBuilder update(String jsonNodePath, Object value);

    JsonBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue);

    JsonBuilder remove(String jsonNodePath);

    JsonBuilder build();

    String toPrettyString();

    JsonNode buildAsJsonNode();

    JsonNode getNodeAt(String jsonNodePath);

    void clean();

    JsonBuilder writeTo(String filePath);

    boolean isBuilderEmpty();

    <T> T transformToPojo(Class<?> classType);

    <T> T transformNodeToPojo(String jsonNodePath, Class<?> classType);

    List<String> extractJsonPaths();

    Map<String, String> extractJsonPathValueMap();

    static JsonNode getNodeAt(JsonNode node, String jsonNodePath) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        return node.at(jsonPath);
    }

    static String getValueAtNodeAsText(JsonNode node, String jsonNodePath) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        return node.at(jsonPath).isMissingNode() ? "null" : node.at(jsonPath).asText();
    }

    static JsonNode transformPojoToJsonNode(Object object) {
        return MAPPER.valueToTree(object);
    }

    static String extractPojoToPretryJsonString(Object object) {
        return generateString(object, true);
    }

    static String extractPojoToJsonString(Object object) {
        return generateString(object, false);
    }

    private static String generateString(Object object, boolean pretty) {
        if (pretty) return MAPPER.valueToTree(object).toPrettyString();
        else return MAPPER.valueToTree(object).toString();
    }

    @SneakyThrows
    static <T> T transformJsonToPojoObject(String json, Class<T> type) {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return MAPPER.readValue(json, type);
    }

    @SneakyThrows
    static <T> List<T> transformJsonToPojoLst(String json, Class<T> type) {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return MAPPER.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, type));
    }

    static String convertJsonNodePathWithSlashSeparator(String jsonPath) {
        if (Objects.nonNull(jsonPath)) {
            jsonPath = StringUtils.replace(jsonPath, ".", "/");
            jsonPath = StringUtils.replace(jsonPath, "[", "/");
            jsonPath = StringUtils.replace(jsonPath, "]", "/");
            jsonPath = StringUtils.replace(jsonPath, "//", "/");
            jsonPath = StringUtils.prependIfMissing(jsonPath, "/");
            jsonPath = StringUtils.removeEnd(jsonPath, "/");
        }
        return jsonPath;
    }

    static boolean isNotSkippable(Object value) {
        if (Objects.nonNull(value)) {
            String stringValue = String.valueOf(value);
            return !stringValue.equalsIgnoreCase(NodeType.SKIP.getType()) && !stringValue.equalsIgnoreCase(NodeType.IGNORE.getType());
        }
        return true;
    }

    static String getJsonPath(JsonNode rootNode, JsonNode subNode) {
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

    static List<String> collectJsonPaths(JsonNode node, String parentPath, List<String> paths) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String currentPath = parentPath.isEmpty() ? entry.getKey() : parentPath + "." + entry.getKey();
                JsonNode childNode = entry.getValue();
                if (childNode.isArray()) {
                    for (int i = 0; i < childNode.size(); i++) {
                        JsonNode arrayElement = childNode.get(i);
                        if (arrayElement.isTextual()) {
                            paths.add(currentPath + "[" + i + "]");
                        } else {
                            collectJsonPaths(arrayElement, currentPath + "[" + i + "]", paths);
                        }
                    }
                } else if (childNode.isObject()) {
                    collectJsonPaths(childNode, currentPath, paths);
                } else {
                    paths.add(currentPath);
                }
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode arrayElement = node.get(i);
                if (arrayElement.isTextual()) {
                    paths.add(parentPath + "[" + i + "]");
                } else {
                    collectJsonPaths(arrayElement, parentPath + "[" + i + "]", paths);
                }
            }
        }
        return paths;
    }

    static Map<String, String> collectJsonPathKeyValuePairs(JsonNode node, String parentPath, LinkedHashMap<String, String> pathKeyValueMap) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String currentPath = parentPath.isEmpty() ? entry.getKey() : parentPath + "." + entry.getKey();
                JsonNode childNode = entry.getValue();
                if (childNode.isArray()) {
                    for (int i = 0; i < childNode.size(); i++) {
                        JsonNode arrayElement = childNode.get(i);
                        if (arrayElement.isTextual()) {
                            pathKeyValueMap.put(currentPath + "[" + i + "]", arrayElement.asText());
                        } else {
                            collectJsonPathKeyValuePairs(arrayElement, currentPath + "[" + i + "]", pathKeyValueMap);
                        }
                    }
                } else if (childNode.isObject()) {
                    collectJsonPathKeyValuePairs(childNode, currentPath, pathKeyValueMap);
                } else {
                    pathKeyValueMap.put(currentPath, childNode.asText());
                }
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode arrayElement = node.get(i);
                if (arrayElement.isTextual()) {
                    pathKeyValueMap.put(parentPath + "[" + i + "]", arrayElement.asText());
                } else {
                    collectJsonPathKeyValuePairs(arrayElement, parentPath + "[" + i + "]", pathKeyValueMap);
                }
            }
        }
        return pathKeyValueMap;
    }

    static JsonNode convertValueOfRequiredDataType(Object value, NodeType valueType) {
        if (value == null) return NullNode.getInstance();

        String stringValue = Objects.toString(value, "");

        return switch (valueType) {
            case INT, LONG, NUMBER, INTEGER -> new LongNode((long) Double.parseDouble(stringValue));
            case DOUBLE, DECIMAL, FLOAT -> new DecimalNode(new BigDecimal(stringValue));
            case BOOLEAN -> BooleanNode.valueOf(Boolean.parseBoolean(stringValue));
            case EMPTY, BLANK -> new TextNode("");
            case NULL -> NullNode.getInstance();
            case EMPTYOBJECT -> MAPPER.createObjectNode();
            case EMPTYARRAY -> MAPPER.createArrayNode();
            case OBJECTNODE ->
                    stringValue.isBlank() ? MAPPER.createObjectNode() : JsonBuilder.objectBuilder().fromJsonString(stringValue).buildAsJsonNode();
            case ARRAYNODE ->
                    stringValue.isBlank() ? MAPPER.createArrayNode() : JsonBuilder.arrayBuilder().fromJsonString(stringValue).buildAsJsonNode();
            default -> new TextNode(stringValue);
        };
    }
}
