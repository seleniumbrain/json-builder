package org.json.builder.core;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.json.builder.exception.JsonBuilderException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

/**
 * A builder class for constructing and manipulating JSON objects.
 * This class provides methods to create JSON objects from various sources,
 * update JSON nodes, and transform JSON nodes to POJOs.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * JsonObjectBuilder builder = new JsonObjectBuilder();
 * builder.fromJsonString("{\"name\":\"John\"}")
 *        .update("age", 30)
 *        .build();
 * String jsonString = builder.toPrettyString();
 * }
 * Output:
 * <pre>
 * {@code
 * {
 *   "name" : "John",
 *   "age" : "30"
 * }
 * }
 * </pre>
 *
 * @author rajkumarrajamani
 * @date 2021-09-26
 */
public class JsonObjectBuilder implements JsonBuilder {

    public static void main(String[] args) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        builder.fromJsonString("{\"name\":\"John\"}").update("age", 30).build();
        String jsonString = builder.toPrettyString();
        System.out.println(jsonString);
    }

    private final Map<String, Object> jsonPathValueMapToAppend = new LinkedHashMap<>();
    private final Map<String, Object> jsonPathValueMapToRemove = new LinkedHashMap<>();
    private ObjectNode rootObjectNode = MAPPER.createObjectNode();

    /**
     * Build JSON data by reading from a json-file.
     *
     * @param jsonFileName the name of the JSON file
     * @return the current instance of JsonObjectBuilder
     * @throws JsonBuilderException if the file name is blank or the file is invalid
     *
     *                              <p>Example usage:</p>
     *                              <pre>
     *                                                                                                                                                                                                                                                                                                   {@code
     *                                                                                                                                                                                                                                                                                                   String jsonFileName = "file-path/data.json";
     *                                                                                                                                                                                                                                                                                                   JsonObjectBuilder builder = new JsonObjectBuilder();
     *                                                                                                                                                                                                                                                                                                   String json = builder.fromJsonFile(jsonFileName).build().toPrettyString();
     *                                                                                                                                                                                                                                                                                                   }
     *                                                                                                                                                                                                                                                                                                   </pre>
     */
    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromJsonFile(String jsonFileName) {
        validateFileName(jsonFileName);
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(new File(jsonFileName));
        return this;
    }

    /**
     * Build JSON data by reading from a json-file.
     *
     * @param jsonFile the JSON file
     * @return the current instance of JsonObjectBuilder
     * @throws JsonBuilderException if the file name is blank or the file is invalid
     *
     *                              <p>Example usage:</p>
     *                              <pre>
     *                                                                                                                                                                                                                                                                                                   {@code
     *                                                                                                                                                                                                                                                                                                   File jsonFile = new File("file-path/data.json");
     *                                                                                                                                                                                                                                                                                                   JsonObjectBuilder builder = new JsonObjectBuilder();
     *                                                                                                                                                                                                                                                                                                   String json = builder.fromJsonFile(jsonFile).build().toPrettyString();
     *                                                                                                                                                                                                                                                                                                   }
     *                                                                                                                                                                                                                                                                                                   </pre>
     */
    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromJsonFile(File jsonFile) {
        validateFile(jsonFile);
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(jsonFile);
        return this;
    }

    /**
     * Build JSON data by reading from a JSON string
     *
     * @param json the JSON string
     * @return the current instance of JsonObjectBuilder
     * @throws JsonBuilderException if the file name is blank or the file is invalid
     *
     *                              <p>Example usage:</p>
     *                              <pre>
     *                                                                                                                                                                                                                                                                                                   {@code
     *                                                                                                                                                                                                                                                                                                   File jsonFile = new File("file-path/data.json");
     *                                                                                                                                                                                                                                                                                                   JsonObjectBuilder builder = new JsonObjectBuilder();
     *                                                                                                                                                                                                                                                                                                   String json = builder.fromJsonString("{\"name\":\"John\"}").build().toPrettyString();
     *                                                                                                                                                                                                                                                                                                   }
     *                                                                                                                                                                                                                                                                                                   </pre>
     */
    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromJsonString(String json) {
        validateJsonString(json);
        this.rootObjectNode = (ObjectNode) MAPPER.readTree(json);
        return this;
    }

    /**
     * Creates an empty JSON object.
     *
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * String json = builder.fromEmptyNode()
     * .update("name", "John")
     * .update("age", 30)
     * .build()
     * .toPrettyString();
     * }</pre>
     */
    @Override
    @SneakyThrows
    public synchronized JsonObjectBuilder fromEmptyNode() {
        this.rootObjectNode = MAPPER.createObjectNode();
        return this;
    }

    /**
     * Updates the value at the specified JSON node path with the given value and data type.
     * If the JSON node path does not exist, it creates a new node with the given value.
     *
     * @param jsonNodePath    the path of the JSON node to update
     * @param value           the value to set at the specified JSON node path
     * @param dataTypeOfValue the data type of the value to set
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\"}")
     *        .update("age", 30, NodeType.INT)
     *        .build();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {
     *   "name" : "John",
     *   "age" : 30
     * }
     * }</pre>
     */
    @Override
    public synchronized JsonObjectBuilder update(String jsonNodePath, Object value, NodeType dataTypeOfValue) {
        if (JsonBuilder.isNotSkippable(value)) {
            jsonPathValueMapToAppend.put(convertPath(jsonNodePath), JsonBuilder.convertValueOfRequiredDataType(value, dataTypeOfValue));
        }
        return this;
    }

    /**
     * Updates the value at the specified JSON node path with the given value as a string.
     * If the JSON node path does not exist, it creates a new node with the given value as a string.
     *
     * @param jsonNodePath the path of the JSON node to update
     * @param value        the value to set at the specified JSON node path
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\"}")
     *        .update("age", 30)
     *        .build();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {
     *   "name" : "John",
     *   "age" : "30"
     * }
     * }</pre>
     */
    @Override
    public synchronized JsonObjectBuilder update(String jsonNodePath, Object value) {
        return update(jsonNodePath, value, NodeType.STRING);
    }

    /**
     * Updates the value of a key in an array node if the specified condition is met.
     * If the condition is met, the value of the key in the array node at the specified path is updated with the new value.
     *
     * @param condition     the condition to check for each element in the array node
     * @param arrayNodePath the path of the array node to update
     * @param key           the key of the value to update in the array node
     * @param newValue      the new value to set if the condition is met
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * Predicate<JsonNode> condition = node -> node.get("name").asText().equals("John");
     * builder.fromJsonString("[{\"name\":\"John\", \"age\":25}, {\"name\":\"Jane\", \"age\":30}]")
     *        .updateArrayNodeIf(condition, "", "age", "26")
     *        .build();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * [
     *   {
     *     "name" : "John",
     *     "age" : "26"
     *   },
     *   {
     *     "name" : "Jane",
     *     "age" : 30
     *   }
     * ]
     * }</pre>
     */
    @Override
    public synchronized JsonObjectBuilder updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue) {
        updateNodeIf(condition, arrayNodePath, key, newValue);
        return this;
    }

    /**
     * Removes the JSON node at the specified path.
     * This method marks the node at the given path for removal.
     *
     * @param jsonNodePath the path of the JSON node to remove
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
     *        .remove("age")
     *        .build();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {
     *   "name" : "John"
     * }
     * }</pre>
     */
    @Override
    public synchronized JsonObjectBuilder remove(String jsonNodePath) {
        jsonPathValueMapToRemove.put(convertPath(jsonNodePath), "");
        return this;
    }

    /**
     * Builds the JSON object by applying all the updates and removals.
     * This method processes the maps of JSON paths to append and remove,
     * applying the changes to the root JSON object node.
     *
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
     *        .update("age", 31)
     *        .remove("name")
     *        .build();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {
     *   "age" : "31"
     * }
     * }</pre>
     */
    @Override
    public synchronized JsonObjectBuilder build() {
        jsonPathValueMapToAppend.forEach((key, value) -> setJsonPointerValue(rootObjectNode, JsonPointer.compile(key), (JsonNode) value));
        jsonPathValueMapToAppend.clear();
        jsonPathValueMapToRemove.forEach((key, value) -> removeJsonPointerValue(rootObjectNode, JsonPointer.compile(key)));
        jsonPathValueMapToRemove.clear();
        return this;
    }

    /**
     * Converts the JSON object to a pretty-printed string.
     * This method builds the JSON object by applying all the updates and removals,
     * and then converts the resulting JSON object to a pretty-printed string format.
     *
     * @return a pretty-printed JSON string representation of the JSON object
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
     *        .update("age", 31)
     *        .remove("name")
     *        .build();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {
     *   "age" : "31"
     * }
     * }</pre>
     */
    @Override
    public synchronized String toPrettyString() {
        build();
        return rootObjectNode.toPrettyString();
    }

    /**
     * Builds the JSON object as a JsonNode by applying all the updates and removals.
     * This method processes the maps of JSON paths to append and remove,
     * applying the changes to the root JSON object node and returning it as a JsonNode.
     *
     * @return the root JSON object node as a JsonNode
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
     *        .update("age", 31)
     *        .remove("name")
     *        .buildAsJsonNode();
     * JsonNode jsonNode = builder.buildAsJsonNode();
     * System.out.println(jsonNode.toPrettyString());
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {
     *   "age" : "31"
     * }
     * }</pre>
     */
    @Override
    public synchronized JsonNode buildAsJsonNode() {
        build();
        validateRootNode();
        return rootObjectNode;
    }

    /**
     * Retrieves the JSON node at the specified path.
     * This method navigates the JSON structure using the given path and returns the node found at that path.
     *
     * @param jsonNodePath the path of the JSON node to retrieve
     * @return the JSON node at the specified path
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}");
     * JsonNode node = builder.getNodeAt("name");
     * System.out.println(node.asText());
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * John
     * }</pre>
     */
    @Override
    public synchronized JsonNode getNodeAt(String jsonNodePath) {
        return rootObjectNode.at(convertPath(jsonNodePath));
    }

    /**
     * Cleans the JSON object builder by removing all nodes and clearing the maps of paths to append and remove.
     * This method resets the builder to an empty state.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
     *        .update("age", 31)
     *        .clean();
     * String jsonString = builder.toPrettyString();
     * System.out.println(jsonString);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * {}
     * }</pre>
     */
    @Override
    public synchronized void clean() {
        rootObjectNode.removeAll();
        jsonPathValueMapToAppend.clear();
        jsonPathValueMapToRemove.clear();
    }

    /**
     * Writes the JSON object to a file at the specified file path.
     * This method builds the JSON object by applying all the updates and removals,
     * and then writes the resulting JSON object to a file in a pretty-printed format.
     *
     * @param filePath the path of the file to write the JSON object to
     * @return the current instance of JsonObjectBuilder
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
     *        .update("age", 31)
     *        .remove("name")
     *        .writeTo("output.json");
     * }</pre>
     *
     * <p>Output file content (output.json):</p>
     * <pre>{@code
     * {
     *   "age" : "31"
     * }
     * }</pre>
     */
    @SneakyThrows
    @Override
    public synchronized JsonObjectBuilder writeTo(String filePath) {
        Files.writeString(Paths.get(filePath), toPrettyString());
        return this;
    }

    /**
     * Checks if the JSON object builder is empty.
     * This method verifies if the root JSON object node is null, empty, or missing.
     *
     * @return true if the builder is empty, false otherwise
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * boolean isEmpty = builder.isBuilderEmpty();
     * System.out.println(isEmpty);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * true
     * }</pre>
     */
    @Override
    synchronized public boolean isBuilderEmpty() {
        return rootObjectNode.isNull() || rootObjectNode.isEmpty() || rootObjectNode.isMissingNode();
    }

    /**
     * Transforms the root JSON object node to a POJO of the specified class type.
     * This method converts the JSON structure represented by the root JSON object node
     * into an instance of the specified class type.
     *
     * @param classType the class type to transform the JSON node into
     * @param <T>       the type of the class modeled by this Class object
     * @return an instance of the specified class type with data populated from the JSON node
     * @throws IOException if there is a problem during the transformation
     *
     *                     <p>Class Structure:</p>
     *                     <pre>{@code
     *                                         public class Person {
     *                                             private String name;
     *                                             private int age;
     *
     *                                             // Getters and Setters
     *                                         }
     *                                         }</pre>
     *
     *                     <p>Example usage:</p>
     *                     <pre>{@code
     *                                         JsonObjectBuilder builder = new JsonObjectBuilder();
     *                                         builder.fromJsonString("{\"name\":\"John\", \"age\":30}");
     *                                         Person person = builder.transformToPojo(Person.class);
     *                                         System.out.println(person.getName());
     *                                         System.out.println(person.getAge());
     *                                         }</pre>
     *
     *                     <p>Output:</p>
     *                     <pre>{@code
     *                                         John
     *                                         30
     *                                         }</pre>
     */
    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    synchronized public <T> T transformToPojo(Class<?> classType) {
        return (T) MAPPER.treeToValue(rootObjectNode, classType);
    }

    /**
     * Transforms the JSON node at the specified path to a POJO of the specified class type.
     * This method converts the JSON structure represented by the node at the given path
     * into an instance of the specified class type.
     *
     * @param jsonNodePath the path of the JSON node to transform, relative to the root object node
     * @param classType    the class type to transform the JSON node into
     * @param <T>          the type of the class modeled by this Class object
     * @return an instance of the specified class type with data populated from the JSON node
     *
     * <p>Class Structure:</p>
     * <pre>{@code
     * public class Person {
     *     private String name;
     *     private int age;
     *
     *     // Getters and Setters
     * }
     * }</pre>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"person\":{\"name\":\"John\", \"age\":30}}");
     * Person person = builder.transformNodeToPojo("person", Person.class);
     * System.out.println(person.getName());
     * System.out.println(person.getAge());
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * John
     * 30
     * }</pre>
     */
    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    synchronized public <T> T transformNodeToPojo(String jsonNodePath, Class<?> classType) {
        return (T) MAPPER.treeToValue(rootObjectNode.at(convertPath(jsonNodePath)), classType);
    }

    /**
     * Extracts all JSON paths from the root JSON object node.
     * This method collects all the paths in the JSON structure represented by the root JSON object node.
     *
     * @return a list of JSON paths as strings
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"address\":{\"city\":\"New York\"}}");
     * List<String> paths = builder.extractJsonPaths();
     * paths.forEach(System.out::println);
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * name
     * address.city
     * }</pre>
     */
    @Override
    synchronized public List<String> extractJsonPaths() {
        return JsonBuilder.collectJsonPaths(rootObjectNode, StringUtils.EMPTY, new ArrayList<>());
    }

    /**
     * Extracts a map of JSON paths and their corresponding values from the root JSON object node.
     * This method collects all the paths and their values in the JSON structure represented by the root JSON object node.
     *
     * @return a map where the keys are JSON paths as strings and the values are the corresponding values as strings
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * JsonObjectBuilder builder = new JsonObjectBuilder();
     * builder.fromJsonString("{\"name\":\"John\", \"address\":{\"city\":\"New York\"}}");
     * Map<String, String> pathValueMap = builder.extractJsonPathValueMap();
     * pathValueMap.forEach((path, value) -> System.out.println(path + ": " + value));
     * }</pre>
     *
     * <p>Output:</p>
     * <pre>{@code
     * name: John
     * address.city: New York
     * }</pre>
     */
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
        if (Objects.isNull(rootObjectNode)) {
            throw new JsonBuilderException("Root node is null.");
        }
    }

    private String convertPath(String jsonNodePath) {
        return JsonBuilder.convertJsonNodePathWithSlashSeparator(jsonNodePath);
    }
}