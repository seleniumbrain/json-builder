# JSON Builder and Validator

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/seleniumbrain/json-builder)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.seleniumbrain/json-builder)](https://central.sonatype.com/artifact/io.github.seleniumbrain/json-builder)


This project provides a set of utilities for building, manipulating, and validating JSON objects in Java. It includes classes for creating JSON objects from various sources, updating JSON nodes, transforming JSON nodes to POJOs, and validating JSON against a set of rules.

## Maven Dependency

To include this project in your Maven build, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.json.builder</groupId>
    <artifactId>json-builder</artifactId>
    <version>1.0.4</version>
</dependency>
```
> [!IMPORTANT]
> Always check for the latest version at [Maven Central Repository](https://central.sonatype.com/artifact/io.github.seleniumbrain/json-builder).

## Tools and Prerequisites

- **Java**: JDK 17 or higher
- **Maven**: 3.6.3 or higher

## Usage

### JsonObjectBuilder

`JsonObjectBuilder` is used to create and manipulate JSON objects.

#### Example

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\"}")
       .update("age", 30)
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

### Methods

## `fromJsonFile(String jsonFileName)`

Build JSON data by reading from a JSON file.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
String jsonFileName = "file-path/data.json";
String json = builder.fromJsonFile(jsonFileName).build().toPrettyString();
System.out.println(json);
```

## `fromJsonFile(File jsonFile)`

Build JSON data by reading from a JSON file.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
File jsonFile = new File("file-path/data.json");
String json = builder.fromJsonFile(jsonFile).build().toPrettyString();
System.out.println(json);
```

## `fromJsonString(String json)`

Build JSON data by reading from a JSON string.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
String json = builder.fromJsonString("{\"name\":\"John\"}").build().toPrettyString();
System.out.println(json);
```

## `fromEmptyNode()`

Creates an empty JSON object.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
String json = builder.fromEmptyNode()
                     .update("name", "John")
                     .update("age", 30)
                     .build()
                     .toPrettyString();
System.out.println(json);
```

## `update(String jsonNodePath, Object value, NodeType dataTypeOfValue)`

Updates the value at the specified JSON node path with the given value and data type.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\"}")
       .update("age", 30, NodeType.INT)
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

## `update(String jsonNodePath, Object value)`

Updates the value at the specified JSON node path with the given value as a string.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\"}")
       .update("age", 30)
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

## `updateArrayNodeIf(Predicate<JsonNode> condition, String arrayNodePath, String key, String newValue)`

Updates the value of a key in an array node if the specified condition is met.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
Predicate<JsonNode> condition = node -> node.get("name").asText().equals("John");
builder.fromJsonString("[{\"name\":\"John\", \"age\":25}, {\"name\":\"Jane\", \"age\":30}]")
       .updateArrayNodeIf(condition, "", "age", "26")
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

## `remove(String jsonNodePath)`

Removes the JSON node at the specified path.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
       .remove("age")
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

## `build()`

Builds the JSON object by applying all the updates and removals.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
       .update("age", 31)
       .remove("name")
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

> [!IMPORTANT]
> #### Ensure that you call this method after reading JSON content either from file or string. OR while removing any node.

## `toPrettyString()`

Converts the JSON object to a pretty-printed string.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
       .update("age", 31)
       .remove("name")
       .build();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

## `buildAsJsonNode()`

Builds the JSON object as a `JsonNode` by applying all the updates and removals.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
       .update("age", 31)
       .remove("name")
       .buildAsJsonNode();
JsonNode jsonNode = builder.buildAsJsonNode();
System.out.println(jsonNode.toPrettyString());
```

## `getNodeAt(String jsonNodePath)`

Retrieves the JSON node at the specified path.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}");
JsonNode node = builder.getNodeAt("name");
System.out.println(node.asText());
```

> [!IMPORTANT]
> Ensure the JSON path always follows '.' separated path. For example, `address[0].city`.

## `clean()`

Cleans the JSON object builder by removing all nodes and clearing the maps of paths to append and remove.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
       .update("age", 31)
       .clean();
String jsonString = builder.toPrettyString();
System.out.println(jsonString);
```

## `writeTo(String filePath)`

Writes the JSON object to a file at the specified file path.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
       .update("age", 31)
       .remove("name")
       .writeTo("output.json");
```

## `isBuilderEmpty()`

Checks if the JSON object builder is empty.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
boolean isEmpty = builder.isBuilderEmpty();
System.out.println(isEmpty);
```

## `transformToPojo(Class<?> classType)`

Transforms the root JSON object node to a POJO of the specified class type.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"age\":30}");
Person person = builder.transformToPojo(Person.class);
System.out.println(person.getName());
System.out.println(person.getAge());
```

## `transformNodeToPojo(String jsonNodePath, Class<?> classType)`

Transforms the JSON node at the specified path to a POJO of the specified class type.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"person\":{\"name\":\"John\", \"age\":30}}");
Person person = builder.transformNodeToPojo("person", Person.class);
System.out.println(person.getName());
System.out.println(person.getAge());
```

## `extractJsonPaths()`

Extracts all JSON paths from the root JSON object node.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"address\":{\"city\":\"New York\"}}");
List<String> paths = builder.extractJsonPaths();
paths.forEach(System.out::println);
```

## `extractJsonPathValueMap()`

Extracts a map of JSON paths and their corresponding values from the root JSON object node.

```java
JsonObjectBuilder builder = JsonBuilder.objectBuilder();
builder.fromJsonString("{\"name\":\"John\", \"address\":{\"city\":\"New York\"}}");
Map<String, String> pathValueMap = builder.extractJsonPathValueMap();
pathValueMap.forEach((path, value) -> System.out.println(path + ": " + value));
```
---

### JsonArrayBuilder

`JsonArrayBuilder` also has the same usages as `JsonObjectBuilder`. The methods and their usage examples are similar, but they operate on JSON arrays instead of JSON objects.

---

## JsonValidator

`JsonValidator` is used to validate JSON against a set of rules defined in a rule book.

#### Example

```java
List<String> validationErrors = JsonValidator.verify("rule-book.json", "actual-json.json");
if (validationErrors.isEmpty()) {
    System.out.println("All validations passed.");
} else {
    validationErrors.forEach(System.out::println);
}
```
---

## JsonPathFinder

`JsonPathFinder` is used to find JSON paths in a JSON object. Finds the JSON path of a sub-node within a root node.

#### Example

```java
JsonNode rootNode = ...; // Initialize root node
JsonNode subNode = ...; // Initialize sub node
String jsonPath = JsonPathFinder.getJsonPath(rootNode, subNode);
System.out.println(jsonPath);
```

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Contact Info

For any questions or issues, please contact:

- **Name**: Rajkumar Rajamani
- **Email**: rajoviyaa.s@gmail.com
