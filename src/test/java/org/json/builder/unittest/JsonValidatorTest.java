package org.json.builder.unittest;

import org.json.builder.exception.JsonBuilderException;
import org.json.builder.helper.JsonValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JsonValidatorTest {

    @Test
    void verify_ValidJsonFiles_ReturnsEmptyList() {
        String ruleBookFile = "src/test/resources/valid-rule-book.json";
        String actualJsonFile = "src/test/resources/valid-actual-json.json";
        List<String> result = JsonValidator.verify(ruleBookFile, actualJsonFile);
        assertTrue(result.isEmpty());
    }

    @Test
    void verify_InvalidJsonFiles_ReturnsFailedRules() {
        String ruleBookFile = "src/test/resources/invalid-rule-book.json";
        String actualJsonFile = "src/test/resources/valid-actual-json.json";
        List<String> result = JsonValidator.verify(ruleBookFile, actualJsonFile);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }

    @Test
    void verify_EmptyRuleBookFile_ReturnsEmptyList() {
        String ruleBookFile = "src/test/resources/empty-rule-book.json";
        String actualJsonFile = "src/test/resources/valid-actual-json.json";
        List<String> result = JsonValidator.verify(ruleBookFile, actualJsonFile);
        assertTrue(result.isEmpty());
    }

    @Test
    void verify_EmptyActualJsonFile_ReturnsFailedRules() {
        String ruleBookFile = "src/test/resources/valid-rule-book.json";
        String actualJsonFile = "src/test/resources/empty-actual-json.json";
        List<String> result = JsonValidator.verify(ruleBookFile, actualJsonFile);
        assertFalse(result.isEmpty());
    }

    @Test
    void verify_NonExistentFiles_ThrowsException() {
        String ruleBookFile = "src/test/resources/non-existent-rule-book.json";
        String actualJsonFile = "src/test/resources/non-existent-actual-json.json";
        assertThrows(JsonBuilderException.class, () -> JsonValidator.verify(ruleBookFile, actualJsonFile));
    }
}