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
 *
 */
public class JsonArrayBuilder implements JsonBuilder {

    private Map<String, Object> jsonPathValueMapToAppend = new LinkedHashMap<>();
    private Map<String, Object> jsonPathValueMapToRemove = new LinkedHashMap<>();
    private ArrayNode rootArrayNode = MAPPER.createArrayNode();

    public JsonArrayBuilder() {
    }

    @Override
    @SneakyThrows
    synchronized public JsonArrayBuilder fromJsonFile(String jsonFileName) {
        if (jsonFileName.isBlank())
            throw new JsonBuilderException("File name isn't provided. Please provide a valid json file name.");

        File file = new File(jsonFileName);
        if (!file.exists() || !file.isFile())
            throw new JsonBuilderException("File doesn't exist. Please provide a valid json file name.");

        this.rootArrayNode = (ArrayNode) MAPPER.readTree(file);
        return this;
    }

    @Override
    @SneakyThrows
    synchronized public JsonArrayBuilder fromJsonFile(File jsonFile) {
        if (!jsonFile.exists() || !jsonFile.isFile())
            throw new JsonBuilderException("File doesn't exist. Please provide a valid json file name.");

        this.rootArrayNode = (ArrayNode) MAPPER.readTree(jsonFile);
        return this;
    }

    @Override
    @SneakyThrows
    synchronized public JsonArrayBuilder fromJsonString(String json) {
        if (Objects.isNull(json) || json.isBlank())
            throw new JsonBuilderException("Json is either null or blank. Please provide a valid json.");

        this.rootArrayNode = (ArrayNode) MAPPER.readTree(json);
        return this;
    }

    @Override
    @SneakyThrows
    synchronized public JsonArrayBuilder withEmptyNode() {
        this.rootArrayNode = MAPPER.createArrayNode();
        return this;
    }

    @Override
    synchronized public JsonArrayBuilder append(String jsonNodePath, Object value, String dataTypeOfValue) {
        if (JsonBuilder.isNotSkippable(value)) {
            String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
            jsonPathValueMapToAppend.put(jsonPath, JsonBuilder.convertValueOfRequiredDataType(value, dataTypeOfValue));
        }
        return this;
    }

    @Override
    synchronized public JsonArrayBuilder append(String jsonNodePath, Object value) {
        if (JsonBuilder.isNotSkippable(value)) {
            String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
            jsonPathValueMapToAppend.put(jsonPath, JsonBuilder.convertValueOfRequiredDataType(value, NodeValueType.STRING.getType()));
        }
        return this;
    }

    @Override
    synchronized public JsonArrayBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue) {
        JsonNode node = this.getNodeAt(arrayNodePath);
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                if (condition.test(node.get(i))) {
                    String pathToUpdate = arrayNodePath + "[" + i + "]." + key;
                    this.append(pathToUpdate, newValue);
                }
            }
        } else {
            throw new JsonBuilderException("Argument arrayNodePath is not an ArrayNode.");
        }
        return this;
    }

    @Override
    synchronized public JsonArrayBuilder updateObjectNodeIf(Predicate<JsonNode> condition, String objectNodePath, String key, String newValue) {
        JsonNode node = this.getNodeAt(objectNodePath);
        if (node.isObject()) {
            if (condition.test(node)) {
                String pathToUpdate = objectNodePath + "." + key;
                this.append(pathToUpdate, newValue);
            }
        } else {
            throw new JsonBuilderException("Argument objectNodePath is not an ObjectNode.");
        }
        return this;
    }

    @Override
    synchronized public JsonArrayBuilder remove(String jsonNodePath) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        jsonPathValueMapToRemove.put(jsonPath, "");
        return this;
    }

    @Override
    synchronized public JsonArrayBuilder build() {
        Optional.ofNullable(jsonPathValueMapToAppend).orElse(new HashMap<>())
                .forEach((key, value) -> setJsonPointerValueInJsonArray(rootArrayNode, JsonPointer.compile(key), (JsonNode) value));
        jsonPathValueMapToAppend.clear();

        Optional.ofNullable(jsonPathValueMapToRemove).orElse(new HashMap<>())
                .forEach((key, value) -> removeJsonPointerValueInJsonArray(rootArrayNode, JsonPointer.compile(key)));
        jsonPathValueMapToRemove.clear();

        return this;
    }

    @SneakyThrows
    @Override
    synchronized public JsonArrayBuilder writeTo(String filePath) {
        String json = this.toPrettyString();
        Files.writeString(Paths.get(filePath), json);
        return this;
    }

    @Override
    synchronized public String toPrettyString() {
        return rootArrayNode.toPrettyString();
    }

    @Override
    synchronized public JsonNode buildAsJsonNode() {
        this.build();
        if (Objects.isNull(rootArrayNode))
            throw new JsonBuilderException("ObjectMapper is empty or null. Please provide input.");
        return rootArrayNode;
    }

    @Override
    synchronized public JsonNode getNodeAt(String jsonNodePath) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        return rootArrayNode.at(jsonPath);
    }

    @Override
    synchronized public void clean() {
        rootArrayNode.removeAll();
        jsonPathValueMapToAppend = new LinkedHashMap<>();
        jsonPathValueMapToRemove = new LinkedHashMap<>();
    }

    @Override
    synchronized public boolean isBuilderEmpty() {
        return rootArrayNode.isNull() || rootArrayNode.isEmpty() || rootArrayNode.isMissingNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    synchronized public <T> T transformToPojo(Class<?> classType) {
        return (T) MAPPER.treeToValue(rootArrayNode, classType);
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    synchronized public <T> T transformNodeToPojo(String jsonNodePath, Class<?> classType) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        return (T) MAPPER.treeToValue(rootArrayNode.at(jsonPath), classType);
    }

    @Override
    synchronized public List<String> extractJsonPaths() {
        return JsonBuilder.printJsonPath(rootArrayNode, StringUtils.EMPTY, new ArrayList<>());
    }

    @Override
    synchronized public Map<String, String> extractJsonPathValueMap() {
        return JsonBuilder.printJsonPathKeyValuePair(rootArrayNode, StringUtils.EMPTY, new HashMap<>());
    }

    synchronized private void setJsonPointerValueInJsonArray(ArrayNode node, JsonPointer pointer, JsonNode value) {
        JsonPointer parentPointer = pointer.head();
        JsonNode parentNode = node.at(parentPointer);
        String fieldName = pointer.last().toString().substring(1);

        if (parentNode.isMissingNode() || parentNode.isNull()) {
            parentNode = StringUtils.isNumeric(fieldName) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
            setJsonPointerValueInJsonArray(node, parentPointer, parentNode); // recursively reconstruct hierarchy
        }

        if (parentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) parentNode;
            int index = Integer.parseInt(fieldName);
            // Expand array in case index is greater than array size
            for (int i = arrayNode.size(); i <= index; i++) {
                arrayNode.addObject();
            }
            arrayNode.set(index, value);
        } else if (parentNode.isObject()) {
            ((ObjectNode) parentNode).set(fieldName, value);
        } else {
            throw new IllegalArgumentException("'" + fieldName + "' can't be set for parent node '" + parentPointer
                    + "' because parent is not a container but " + parentNode.getNodeType().name());
        }
    }

    synchronized private void removeJsonPointerValueInJsonArray(ArrayNode node, JsonPointer pointer) {
        JsonPointer parentPointer = pointer.head();
        JsonNode parentNode = node.at(parentPointer);
        String fieldName = pointer.last().toString().substring(1);

        if (parentNode.isMissingNode() || parentNode.isNull()) {
            return;
        }

        if (parentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) parentNode;
            int index = Integer.parseInt(fieldName);
            arrayNode.remove(index);
        } else if (parentNode.isObject()) {
            ((ObjectNode) parentNode).remove(fieldName);
        } else {
            throw new IllegalArgumentException("'" + fieldName + "' can't be set for parent node '" + parentPointer
                    + "' because parent is not a container but " + parentNode.getNodeType().name());
        }
    }
}
