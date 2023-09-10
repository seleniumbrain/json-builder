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
import java.util.*;
import java.util.function.Predicate;


/**
 * @author rajkumarrajamani
 */
public class JsonObjectBuilder implements JsonBuilder {

    private Map<String, Object> jsonPathValueMapToAppend = new LinkedHashMap<>();
    private Map<String, Object> jsonPathValueMapToRemove = new LinkedHashMap<>();
    private ObjectNode rootObjectNode = MAPPER.createObjectNode();

    public JsonObjectBuilder() {}

    @Override
    @SneakyThrows
    public JsonObjectBuilder fromJsonFile(String jsonFileName) {
        if (jsonFileName.isBlank())
            throw new JsonBuilderException("File name isn't provided. Please provide a valid json file name.");

        File file = new File(jsonFileName);
        if (!file.exists() || !file.isFile())
            throw new JsonBuilderException("File doesn't exist. Please provide a valid json file name.");

        this.rootObjectNode = (ObjectNode) MAPPER.readTree(file);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonObjectBuilder fromJsonFile(File jsonFile) {
        if (!jsonFile.exists() || !jsonFile.isFile())
            throw new JsonBuilderException("File doesn't exist. Please provide a valid json file name.");

        this.rootObjectNode = (ObjectNode) MAPPER.readTree(jsonFile);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonObjectBuilder fromJsonString(String json) {
        if (Objects.isNull(json) || json.isBlank())
            throw new JsonBuilderException("Json is either null or blank. Please provide a valid json.");

        this.rootObjectNode = (ObjectNode) MAPPER.readTree(json);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonObjectBuilder withEmptyJsonObject() {
        String emptyJsonObjectStyle = "{}";
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(emptyJsonObjectStyle);
        return this;
    }

    @Override
    public JsonObjectBuilder append(String jsonNodePath, Object value, String dataTypeOfValue) {
        if (JsonBuilder.isNotSkippable(value)) {
            String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
            jsonPathValueMapToAppend.put(jsonPath, JsonBuilder.convertValueOfRequiredDataType(value, dataTypeOfValue));
        }
        return this;
    }

    @Override
    public JsonObjectBuilder append(String jsonNodePath, Object value) {
        if (JsonBuilder.isNotSkippable(value)) {
            String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
            jsonPathValueMapToAppend.put(jsonPath, JsonBuilder.convertValueOfRequiredDataType(value, NodeValueType.STRING.getType()));
        }
        return this;
    }

    public JsonObjectBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue) {
        JsonNode node = this.getNodeAt(arrayNodePath);
        if(node.isArray()) {
            for(int i = 0 ; i < node.size() ; i++) {
                if(condition.test(node.get(i))) {
                    String pathToUpdate = arrayNodePath + "[" + i + "]." + key;
                    this.append(pathToUpdate, newValue);
                }
            }
        } else {
            throw new JsonBuilderException("Argument arrayNodePath is not an ArrayNode.");
        }
        return this;
    }

    public JsonObjectBuilder updateObjectNodeIf(Predicate<JsonNode> condition, String objectNodePath, String key, String newValue) {
        JsonNode node = this.getNodeAt(objectNodePath);
        if(node.isObject()) {
            if(condition.test(node)) {
                String pathToUpdate = objectNodePath + "." + key;
                this.append(pathToUpdate, newValue);
            }
        } else {
            throw new JsonBuilderException("Argument objectNodePath is not an ObjectNode.");
        }
        return this;
    }

    @Override
    public JsonObjectBuilder remove(String jsonNodePath) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        jsonPathValueMapToRemove.put(jsonPath, "");
        return this;
    }

    @Override
    public JsonObjectBuilder build() {
        Optional.ofNullable(jsonPathValueMapToAppend).orElse(new HashMap<>())
                .forEach((key, value) -> setJsonPointerValueInJsonObject(rootObjectNode, JsonPointer.compile(key), (JsonNode) value));
        jsonPathValueMapToAppend.clear();

        Optional.ofNullable(jsonPathValueMapToRemove).orElse(new HashMap<>())
                .forEach((key, value) -> removeJsonPointerValueInJsonObject(rootObjectNode, JsonPointer.compile(key)));
        jsonPathValueMapToRemove.clear();

        return this;
    }

    @Override
    public String toPrettyString() {
        return rootObjectNode.toPrettyString();
    }

    @Override
    public JsonNode buildAsJsonNode() {
        this.build();
        if (Objects.isNull(rootObjectNode))
            throw new JsonBuilderException("ObjectMapper is empty or null. Please provide input.");
        return rootObjectNode;
    }

    @Override
    public JsonNode getNodeAt(String jsonNodePath) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        return rootObjectNode.at(jsonPath);
    }

    @Override
    public void clean() {
        rootObjectNode.removeAll();
        jsonPathValueMapToAppend = new LinkedHashMap<>();
        jsonPathValueMapToRemove = new LinkedHashMap<>();
    }

    @Override
    public boolean isBuilderEmpty() {
        return rootObjectNode.isNull() || rootObjectNode.isEmpty() || rootObjectNode.isMissingNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T transformToPojo(Class<?> classType) {
        return (T) MAPPER.treeToValue(rootObjectNode, classType);
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T transformNodeToPojo(String jsonNodePath, Class<?> classType) {
        String jsonPath = JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
        return (T) MAPPER.treeToValue(rootObjectNode.at(jsonPath), classType);
    }

    @Override
    public List<String> extractJsonPaths() {
        return JsonBuilder.printJsonPath(rootObjectNode, StringUtils.EMPTY, new ArrayList<>());
    }

    @Override
    public Map<String, String> extractJsonPathValueMap() {
        return JsonBuilder.printJsonPathKeyValuePair(rootObjectNode, StringUtils.EMPTY, new HashMap<>());
    }


    private void setJsonPointerValueInJsonObject(ObjectNode node, JsonPointer pointer, JsonNode value) {
        JsonPointer parentPointer = pointer.head();
        JsonNode parentNode = node.at(parentPointer);
        String fieldName = pointer.last().toString().substring(1);

        if (parentNode.isMissingNode() || parentNode.isNull()) {
            parentNode = StringUtils.isNumeric(fieldName) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
            setJsonPointerValueInJsonObject(node, parentPointer, parentNode); // recursively reconstruct hierarchy
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

    private void removeJsonPointerValueInJsonObject(ObjectNode node, JsonPointer pointer) {
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
