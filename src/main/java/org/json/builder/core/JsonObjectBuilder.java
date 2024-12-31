/**
 *
 */
package org.json.builder.core;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.json.builder.exception.JsonBuilderException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;


/**
 * @author rajkumarrajamani
 */
public class JsonObjectBuilder implements JsonBuilder {

    public static void main(String[] args) {
        JsonObjectBuilder jsonObjectBuilder = new JsonObjectBuilder();
        String json = jsonObjectBuilder.fromEmptyNode()
                .update("name", "John Doe")
                .update("age", "8738398.343", NodeType.FLOAT)
                .update("isStudent", false)
                .update("marks", 90.439843948439123456, NodeType.DECIMAL)
                .update("subjects", JsonBuilder.transformPojoToJsonNode(List.of("Maths", "Science", "English")).toPrettyString(), NodeType.ARRAYNODE)
                .update("address", JsonBuilder.transformPojoToJsonNode(Map.of("city", "New York", "state", "NY")).toPrettyString(), NodeType.OBJECTNODE)
                .toPrettyString();
        System.out.println(json);
        JsonBuilder.collectJsonPathKeyValuePairs(jsonObjectBuilder.buildAsJsonNode(), StringUtils.EMPTY, new LinkedHashMap<>()).forEach((key, value) -> System.out.println(key + " : " + value));

        String path = jsonObjectBuilder.buildAsJsonNode().findParents("city").toString();
        System.out.println(path);
    }

    private final Map<String, Object> jsonPathValueMapToAppend = new LinkedHashMap<>();
    private final Map<String, Object> jsonPathValueMapToRemove = new LinkedHashMap<>();
    private ObjectNode rootObjectNode = MAPPER.createObjectNode();

    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromJsonFile(String jsonFileName) {
        validateFileName(jsonFileName);
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(new File(jsonFileName));
        return this;
    }

    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromJsonFile(File jsonFile) {
        validateFile(jsonFile);
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(jsonFile);
        return this;
    }

    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromJsonString(String json) {
        validateJsonString(json);
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(json);
        return this;
    }

    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromEmptyNode() {
        this.rootObjectNode = MAPPER.createObjectNode();
        return this;
    }

    @Override
    public synchronized JsonObjectBuilder update(String jsonNodePath, Object value, NodeType dataTypeOfValue) {
        if (JsonBuilder.isNotSkippable(value)) {
            jsonPathValueMapToAppend.put(convertPath(jsonNodePath), JsonBuilder.convertValueOfRequiredDataType(value, dataTypeOfValue));
        }
        return this;
    }

    @Override
    public synchronized JsonObjectBuilder update(String jsonNodePath, Object value) {
        return update(jsonNodePath, value, NodeType.STRING);
    }

    @Override
    public synchronized JsonObjectBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue) {
        updateNodeIf(condition, arrayNodePath, key, newValue);
        return this;
    }

    @Override
    public synchronized JsonObjectBuilder remove(String jsonNodePath) {
        jsonPathValueMapToRemove.put(convertPath(jsonNodePath), "");
        return this;
    }

    @Override
    public synchronized JsonObjectBuilder build() {
        jsonPathValueMapToAppend.forEach((key, value) -> setJsonPointerValue(rootObjectNode, JsonPointer.compile(key), (JsonNode) value));
        jsonPathValueMapToAppend.clear();
        jsonPathValueMapToRemove.forEach((key, value) -> removeJsonPointerValue(rootObjectNode, JsonPointer.compile(key)));
        jsonPathValueMapToRemove.clear();
        return this;
    }

    @Override
    public synchronized String toPrettyString() {
        build();
        return rootObjectNode.toPrettyString();
    }

    @Override
    public synchronized JsonNode buildAsJsonNode() {
        build();
        validateRootNode();
        return rootObjectNode;
    }

    @Override
    public synchronized JsonNode getNodeAt(String jsonNodePath) {
        return rootObjectNode.at(convertPath(jsonNodePath));
    }

    @Override
    public synchronized void clean() {
        rootObjectNode.removeAll();
        jsonPathValueMapToAppend.clear();
        jsonPathValueMapToRemove.clear();
    }

    @SneakyThrows
    @Override
    public synchronized JsonObjectBuilder writeTo(String filePath) {
        Files.writeString(Paths.get(filePath), toPrettyString());
        return this;
    }

    @Override
    synchronized public boolean isBuilderEmpty() {
        return rootObjectNode.isNull() || rootObjectNode.isEmpty() || rootObjectNode.isMissingNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    synchronized public <T> T transformToPojo(Class<?> classType) {
        return (T) MAPPER.treeToValue(rootObjectNode, classType);
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    synchronized public <T> T transformNodeToPojo(String jsonNodePath, Class<?> classType) {
        return (T) MAPPER.treeToValue(rootObjectNode.at(convertPath(jsonNodePath)), classType);
    }

    @Override
    synchronized public List<String> extractJsonPaths() {
        return JsonBuilder.collectJsonPaths(rootObjectNode, StringUtils.EMPTY, new ArrayList<>());
    }

    @Override
    synchronized public Map<String, String> extractJsonPathValueMap() {
        return JsonBuilder.collectJsonPathKeyValuePairs(rootObjectNode, StringUtils.EMPTY, new LinkedHashMap<>());
    }


    private void setJsonPointerValue(ObjectNode node, JsonPointer pointer, JsonNode value) {
        JsonPointer parentPointer = pointer.head();
        JsonNode parentNode = node.at(parentPointer);
        String fieldName = pointer.last().toString().substring(1);

        if (parentNode.isMissingNode() || parentNode.isNull()) {
            parentNode = StringUtils.isNumeric(fieldName) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
            setJsonPointerValue(node, parentPointer, parentNode);
        }

        if (parentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) parentNode;
            int index = Integer.parseInt(fieldName);
            for (int i = arrayNode.size(); i <= index; i++) {
                arrayNode.addObject();
            }
            arrayNode.set(index, value);
        } else if (parentNode.isObject()) {
            ((ObjectNode) parentNode).set(fieldName, value);
        } else {
            throw new IllegalArgumentException("Invalid parent node type for field: " + fieldName);
        }
    }

    private void removeJsonPointerValue(ObjectNode node, JsonPointer pointer) {
        JsonPointer parentPointer = pointer.head();
        JsonNode parentNode = node.at(parentPointer);
        String fieldName = pointer.last().toString().substring(1);

        if (parentNode.isMissingNode() || parentNode.isNull()) {
            return;
        }

        if (parentNode.isArray()) {
            ((ArrayNode) parentNode).remove(Integer.parseInt(fieldName));
        } else if (parentNode.isObject()) {
            ((ObjectNode) parentNode).remove(fieldName);
        } else {
            throw new IllegalArgumentException("Invalid parent node type for field: " + fieldName);
        }
    }

//    private void updateNodeIf(Predicate<JsonNode> condition, String nodePath, String key, String newValue, boolean isArray) {
//        JsonNode node = getNodeAt(nodePath);
//        if (isArray && node.isArray()) {
//            for (int i = 0; i < node.size(); i++) {
//                if (condition.test(node.get(i))) {
//                    update(nodePath + "[" + i + "]." + key, newValue);
//                }
//            }
//        } else if (!isArray && node.isObject()) {
//            if (condition.test(node)) {
//                update(nodePath + "." + key, newValue);
//            }
//        } else {
//            throw new JsonBuilderException("Invalid node type for path: " + nodePath);
//        }
//    }

        private void updateNodeIf(Predicate<JsonNode> condition, String nodePath, String targetNodePath, String newValue) {
        JsonNode node = getNodeAt(nodePath);
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                if (condition.test(node.get(i))) {
                    update(nodePath + "[" + i + "]." + targetNodePath, newValue);
                }
            }
        } else if (node.isObject()) {
            if (condition.test(node)) {
                update(nodePath + "." + targetNodePath, newValue);
            }
        } else {
            throw new JsonBuilderException("Invalid node type for path: " + nodePath);
        }
    }

    // write a method to return json path of the given JsonNode input object
//    public String getJsonPath(JsonNode node) {
//        return JsonBuilder.getJsonPath(rootObjectNode, node); // help me solve this method
//    }

    private void validateFileName(String fileName) {
        if (fileName.isBlank()) {
            throw new JsonBuilderException("File name is blank.");
        }
    }

    private void validateFile(File file) {
        if (!file.exists() || !file.isFile()) {
            throw new JsonBuilderException("Invalid file.");
        }
    }

    private void validateJsonString(String json) {
        if (Objects.isNull(json) || json.isBlank()) {
            throw new JsonBuilderException("Invalid JSON string.");
        }
    }

    private void validateRootNode() {
        if (Objects.isNull(rootObjectNode)) {
            throw new JsonBuilderException("Root node is null.");
        }
    }

    private String convertPath(String jsonNodePath) {
        return JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
    }
}
