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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author rajkumarrajamani
 */
public interface JsonBuilder {

    ObjectMapper MAPPER = new ObjectMapper();

    static JsonBuilder getObjectBuilder() {
        return BuilderFactory.getBuilder("JsonObjectBuilder");
    }

    static JsonBuilder getArrayBuilder() {
        return BuilderFactory.getBuilder("JsonArrayBuilder");
    }

    JsonBuilder fromJsonFile(String jsonFileName);

    JsonBuilder fromJsonFile(File jsonFile);

    JsonBuilder fromJsonString(String json);

    JsonBuilder withEmptyNode();

    JsonBuilder append(String jsonNodePath, Object value, String dataTypeOfValue);

    JsonBuilder append(String jsonNodePath, Object value);

    JsonBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue);

    JsonBuilder updateObjectNodeIf(Predicate<JsonNode> condition, String objectNodePath, String key, String newValue);

    JsonBuilder remove(String jsonNodePath);

    JsonBuilder build();

    String toPrettyString();

    JsonNode buildAsJsonNode();

    JsonNode getNodeAt(String jsonNodePath);

    void clean();

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

    static String convertJsonNodePathWithSlashSeparator(String jsonNodePath) {
        if (Objects.nonNull(jsonNodePath)) {
            jsonNodePath = StringUtils.replace(jsonNodePath, ".", "/");
            jsonNodePath = StringUtils.replace(jsonNodePath, "[", "/");
            jsonNodePath = StringUtils.replace(jsonNodePath, "]", "/");
            jsonNodePath = StringUtils.replace(jsonNodePath, "//", "/");
            jsonNodePath = StringUtils.prependIfMissing(jsonNodePath, "/");
            jsonNodePath = StringUtils.removeEnd(jsonNodePath, "/");
        }
        return jsonNodePath;
    }

    static boolean isNotSkippable(Object value) {
        if (Objects.nonNull(value)) {
            String stringValue = String.valueOf(value);
            return !stringValue.equalsIgnoreCase(NodeValueType.SKIP.getType()) && !stringValue.equalsIgnoreCase(NodeValueType.IGNORE.getType());
        }
        return true;
    }

    static List<String> printJsonPath(JsonNode jsonNode, String parentPath, List<String> pathCollections) {
        if (jsonNode.isObject()) {
            final Iterator<Entry<String, JsonNode>> iterator = jsonNode.fields();
            while (iterator.hasNext()) {
                final Entry<String, JsonNode> innerNode = iterator.next();
                final JsonNode valueNode = innerNode.getValue();
                final String path = parentPath.isBlank() ? innerNode.getKey() : parentPath + "." + innerNode.getKey();

                if (valueNode.isArray()) {
                    for (int i = 0; i < valueNode.size(); i++) {
                        printJsonPath(valueNode.get(i), path + "[" + i + "]", pathCollections);
                    }
                } else if (valueNode.isObject()) {
                    printJsonPath(valueNode, path, pathCollections);
                } else {
                    pathCollections.add(path);
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                printJsonPath(jsonNode.get(i), parentPath + "[" + i + "]", pathCollections);
            }
        }
        return pathCollections;
    }

    static Map<String, String> printJsonPathKeyValuePair(JsonNode jsonNode, String parentPath, Map<String, String> pathKeyValueMap) {
        if (jsonNode.isObject()) {
            final Iterator<Entry<String, JsonNode>> iterator = jsonNode.fields();
            while (iterator.hasNext()) {
                final Entry<String, JsonNode> innerNode = iterator.next();
                final JsonNode valueNode = innerNode.getValue();
                final String path = parentPath.isBlank() ? innerNode.getKey() : parentPath + "." + innerNode.getKey();

                if (valueNode.isArray()) {
                    for (int i = 0; i < valueNode.size(); i++) {
                        printJsonPathKeyValuePair(valueNode.get(i), path + "[" + i + "]", pathKeyValueMap);
                    }
                } else if (valueNode.isObject()) {
                    printJsonPathKeyValuePair(valueNode, path, pathKeyValueMap);
                } else {
                    pathKeyValueMap.put(path, valueNode.asText());
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                printJsonPathKeyValuePair(jsonNode.get(i), parentPath + "[" + i + "]", pathKeyValueMap);
            }
        }
        return pathKeyValueMap;
    }

    static JsonNode convertValueOfRequiredDataType(Object value, String valueType) {

        String stringValue = "";

        if (Objects.nonNull(value)) stringValue = String.valueOf(value);

        if (valueType.equalsIgnoreCase(NodeValueType.INT.getType()) || valueType.equalsIgnoreCase(NodeValueType.NUMBER.getType()) || valueType.equalsIgnoreCase(NodeValueType.INTEGER.getType()))
            return new IntNode(Integer.parseInt(stringValue));
        else if (valueType.equalsIgnoreCase(NodeValueType.LONG.getType()))
            return new LongNode(Long.parseLong(stringValue));
        else if (valueType.equalsIgnoreCase(NodeValueType.DOUBLE.getType()) || valueType.equalsIgnoreCase(NodeValueType.DECIMAL.getType()) || valueType.equalsIgnoreCase(NodeValueType.FLOAT.getType()))
            return new DoubleNode(Double.parseDouble(stringValue));
        else if (valueType.equalsIgnoreCase(NodeValueType.BOOLEAN.getType()))
            return BooleanNode.valueOf(Boolean.parseBoolean(stringValue));
        else if (valueType.equalsIgnoreCase(NodeValueType.STRING.getType()) || valueType.equalsIgnoreCase(NodeValueType.TEXT.getType())) {
            if (Objects.isNull(value)) return null;
            else if (String.valueOf(value).equalsIgnoreCase(NodeValueType.BLANK.getType()) || String.valueOf(value).equalsIgnoreCase(NodeValueType.EMPTY.getType()))
                return new TextNode("");
            else return new TextNode(stringValue);
        } else if (valueType.equalsIgnoreCase(NodeValueType.NULL.getType())) return null;
        else if (valueType.equalsIgnoreCase(NodeValueType.OBJECTNODE.getType()))
            return new ObjectMapper().createObjectNode();
        else if (valueType.equalsIgnoreCase(NodeValueType.ARRAYNODE.getType()))
            return new ObjectMapper().createArrayNode();
        else if (valueType.equalsIgnoreCase(NodeValueType.OBJECT_STYLE_JSON.getType())) {
            if (!stringValue.isBlank()) return JsonBuilder.getObjectBuilder().fromJsonString(stringValue).buildAsJsonNode();
            else return new ObjectMapper().createObjectNode();
        } else if (valueType.equalsIgnoreCase(NodeValueType.ARRAY_STYLE_JSON.getType())) {
            if (!stringValue.isBlank()) return JsonBuilder.getArrayBuilder().fromJsonString(stringValue).buildAsJsonNode();
            else return new ObjectMapper().createArrayNode();
        } else return new TextNode(stringValue);
    }
}
