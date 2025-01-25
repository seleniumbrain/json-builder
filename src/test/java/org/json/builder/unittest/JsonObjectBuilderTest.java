package org.json.builder.unittest;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.builder.bean.Person;
import org.json.builder.core.JsonObjectBuilder;
import org.json.builder.core.NodeType;
import org.json.builder.exception.JsonBuilderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonObjectBuilderTest {

    private JsonObjectBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new JsonObjectBuilder();
    }

    @Test
    void fromJsonFile_withValidFileName_shouldBuildJsonObject() {
        builder.fromJsonFile("src/test/resources/sample.json");
        assertFalse(builder.isBuilderEmpty());
    }

    @Test
    void fromJsonFile_withInvalidFileName_shouldThrowException() {
        assertThrows(JsonBuilderException.class, () -> builder.fromJsonFile(""));
    }

    @Test
    void fromJsonFile_withValidFile_shouldBuildJsonObject() {
        File file = new File("src/test/resources/sample.json");
        builder.fromJsonFile(file);
        assertFalse(builder.isBuilderEmpty());
    }

    @Test
    void fromJsonFile_withInvalidFile_shouldThrowException() {
        File file = new File("invalid/path.json");
        assertThrows(JsonBuilderException.class, () -> builder.fromJsonFile(file));
    }

    @Test
    void fromJsonString_withValidJson_shouldBuildJsonObject() {
        builder.fromJsonString("{\"name\":\"John\"}");
        assertFalse(builder.isBuilderEmpty());
    }

    @Test
    void fromJsonString_withInvalidJson_shouldThrowException() {
        assertThrows(JsonBuilderException.class, () -> builder.fromJsonString(""));
    }

    @Test
    void fromEmptyNode_shouldCreateEmptyJsonObject() {
        builder.fromEmptyNode();
        assertTrue(builder.isBuilderEmpty());
    }

    @Test
    void update_withValidPathAndValue_shouldUpdateJsonObject() {
        builder.fromJsonString("{\"name\":\"John\"}")
               .update("age", 30, NodeType.INT)
               .build();
        JsonNode node = builder.getNodeAt("age");
        assertEquals(30, node.asInt());
    }

    @Test
    void update_withNonExistentPath_shouldCreateNewNode() {
        builder.fromJsonString("{\"name\":\"John\"}")
               .update("address.city", "New York")
               .build();
        JsonNode node = builder.getNodeAt("address.city");
        assertEquals("New York", node.asText());
    }

//    @Test
//    void updateArrayNodeIf_withConditionMet_shouldUpdateArrayNode() {
//        Predicate<JsonNode> condition = node -> node.get("name").asText().equals("John");
//        builder.fromJsonString("[{\"name\":\"John\", \"age\":25}, {\"name\":\"Jane\", \"age\":30}]")
//               .updateArrayNodeIf(condition, "", "age", "26")
//               .build();
//        JsonNode node = builder.getNodeAt("[0].age");
//        assertEquals("26", node.asText());
//    }

    @Test
    void remove_withValidPath_shouldRemoveNode() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
               .remove("age")
               .build();
        JsonNode node = builder.getNodeAt("age");
        assertTrue(node.isMissingNode());
    }

    @Test
    void build_shouldApplyAllUpdatesAndRemovals() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
               .update("age", 31)
               .remove("name")
               .build();
        JsonNode node = builder.buildAsJsonNode();
        assertTrue(node.has("age"));
        assertFalse(node.has("name"));
    }

    @Test
    void toPrettyString_shouldReturnPrettyPrintedJsonString() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
               .update("age", "31")
               .remove("name")
               .build();
        String jsonString = builder.toPrettyString();
        assertTrue(jsonString.contains("\"age\" : \"31\""));
        assertFalse(jsonString.contains("\"name\""));
    }

    @Test
    void buildAsJsonNode_shouldReturnJsonNode() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
               .update("age", 31)
               .remove("name")
               .build();
        JsonNode node = builder.buildAsJsonNode();
        assertTrue(node.has("age"));
        assertFalse(node.has("name"));
    }

    @Test
    void getNodeAt_withValidPath_shouldReturnNode() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}");
        JsonNode node = builder.getNodeAt("name");
        assertEquals("John", node.asText());
    }

    @Test
    void clean_shouldResetBuilder() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
               .update("age", 31)
               .clean();
        assertTrue(builder.isBuilderEmpty());
    }

    @Test
    void writeTo_shouldWriteJsonToFile() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}")
               .update("age", 31)
               .remove("name")
               .writeTo("output.json");
        File file = new File("output.json");
        assertTrue(file.exists());
        file.delete();
    }

    @Test
    void isBuilderEmpty_withEmptyBuilder_shouldReturnTrue() {
        assertTrue(builder.isBuilderEmpty());
    }

    @Test
    void isBuilderEmpty_withNonEmptyBuilder_shouldReturnFalse() {
        builder.fromJsonString("{\"name\":\"John\"}");
        assertFalse(builder.isBuilderEmpty());
    }

    @Test
    void transformToPojo_withValidClass_shouldReturnPojo() {
        builder.fromJsonString("{\"name\":\"John\", \"age\":30}");
        Person person = builder.transformToPojo(Person.class);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    void transformNodeToPojo_withValidPathAndClass_shouldReturnPojo() {
        builder.fromJsonString("{\"person\":{\"name\":\"John\", \"age\":30}}");
        Person person = builder.transformNodeToPojo("person", Person.class);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    void extractJsonPaths_shouldReturnAllPaths() {
        builder.fromJsonString("{\"name\":\"John\", \"address\":{\"city\":\"New York\"}}");
        List<String> paths = builder.extractJsonPaths();
        assertTrue(paths.contains("name"));
        assertTrue(paths.contains("address.city"));
    }

    @Test
    void extractJsonPathValueMap_shouldReturnAllPathsAndValues() {
        builder.fromJsonString("{\"name\":\"John\", \"address\":{\"city\":\"New York\"}}");
        Map<String, String> pathValueMap = builder.extractJsonPathValueMap();
        assertEquals("John", pathValueMap.get("name"));
        assertEquals("New York", pathValueMap.get("address.city"));
    }
}