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

public class JsonArrayBuilder implements JsonBuilder {

    private final Map<String, Object> jsonPathValueMapToAppend = new LinkedHashMap<>();
    private final Map<String, Object> jsonPathValueMapToRemove = new LinkedHashMap<>();
    private ArrayNode rootArrayNode = MAPPER.createArrayNode();

    @Override
    @SneakyThrows
    public synchronized JsonArrayBuilder fromJsonFile(String jsonFileName) {
        validateFileName(jsonFileName);
        this.rootArrayNode = (ArrayNode) MAPPER.readTree(new File(jsonFileName));
        return this;
    }

    @Override
    @SneakyThrows
    public synchronized JsonArrayBuilder fromJsonFile(File jsonFile) {
        validateFile(jsonFile);
        this.rootArrayNode = (ArrayNode) MAPPER.readTree(jsonFile);
        return this;
    }

    @Override
    @SneakyThrows
    public synchronized JsonArrayBuilder fromJsonString(String json) {
        validateJsonString(json);
        this.rootArrayNode = (ArrayNode) MAPPER.readTree(json);
        return this;
    }

    @Override
    @SneakyThrows
    public synchronized JsonArrayBuilder fromEmptyNode() {
        this.rootArrayNode = MAPPER.createArrayNode();
        return this;
    }

    @Override
    public synchronized JsonArrayBuilder update(String jsonNodePath, Object value, NodeType dataTypeOfValue) {
        if (JsonBuilder.isNotSkippable(value)) {
            jsonPathValueMapToAppend.put(convertPath(jsonNodePath), JsonBuilder.convertValueOfRequiredDataType(value, dataTypeOfValue));
        }
        return this;
    }

    @Override
    public synchronized JsonArrayBuilder update(String jsonNodePath, Object value) {
        return update(jsonNodePath, value, NodeType.STRING);
    }

    @Override
    public synchronized JsonArrayBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue) {
        updateNodeIf(condition, arrayNodePath, key, newValue);
        return this;
    }

    @Override
    public synchronized JsonArrayBuilder remove(String jsonNodePath) {
        jsonPathValueMapToRemove.put(convertPath(jsonNodePath), "");
        return this;
    }

    @Override
    public synchronized JsonArrayBuilder build() {
        jsonPathValueMapToAppend.forEach((key, value) -> setJsonPointerValueInJsonArray(rootArrayNode, JsonPointer.compile(key), (JsonNode) value));
        jsonPathValueMapToAppend.clear();
        jsonPathValueMapToRemove.forEach((key, value) -> removeJsonPointerValueInJsonArray(rootArrayNode, JsonPointer.compile(key)));
        jsonPathValueMapToRemove.clear();
        return this;
    }

    @Override
    public synchronized String toPrettyString() {
        build();
        return rootArrayNode.toPrettyString();
    }

    @Override
    public synchronized JsonNode buildAsJsonNode() {
        build();
        validateRootNode();
        return rootArrayNode;
    }

    @Override
    public synchronized JsonNode getNodeAt(String jsonNodePath) {
        return rootArrayNode.at(convertPath(jsonNodePath));
    }

    @Override
    public synchronized void clean() {
        rootArrayNode.removeAll();
        jsonPathValueMapToAppend.clear();
        jsonPathValueMapToRemove.clear();
    }

    @SneakyThrows
    @Override
    public synchronized JsonArrayBuilder writeTo(String filePath) {
        Files.writeString(Paths.get(filePath), toPrettyString());
        return this;
    }

    @Override
    public synchronized boolean isBuilderEmpty() {
        return rootArrayNode.isNull() || rootArrayNode.isEmpty() || rootArrayNode.isMissingNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public synchronized <T> T transformToPojo(Class<?> classType) {
        return (T) MAPPER.treeToValue(rootArrayNode, classType);
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public synchronized <T> T transformNodeToPojo(String jsonNodePath, Class<?> classType) {
        return (T) MAPPER.treeToValue(rootArrayNode.at(convertPath(jsonNodePath)), classType);
    }

    @Override
    public synchronized List<String> extractJsonPaths() {
        return JsonBuilder.collectJsonPaths(rootArrayNode, StringUtils.EMPTY, new ArrayList<>());
    }

    @Override
    public synchronized Map<String, String> extractJsonPathValueMap() {
        return JsonBuilder.collectJsonPathKeyValuePairs(rootArrayNode, StringUtils.EMPTY, new LinkedHashMap<>());
    }

    private void setJsonPointerValueInJsonArray(ArrayNode node, JsonPointer pointer, JsonNode value) {
        JsonPointer parentPointer = pointer.head();
        JsonNode parentNode = node.at(parentPointer);
        String fieldName = pointer.last().toString().substring(1);

        if (parentNode.isMissingNode() || parentNode.isNull()) {
            parentNode = StringUtils.isNumeric(fieldName) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
            setJsonPointerValueInJsonArray(node, parentPointer, parentNode);
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

    private void removeJsonPointerValueInJsonArray(ArrayNode node, JsonPointer pointer) {
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
        if (Objects.isNull(rootArrayNode)) {
            throw new JsonBuilderException("Root node is null.");
        }
    }

    private String convertPath(String jsonNodePath) {
        return JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
    }
}