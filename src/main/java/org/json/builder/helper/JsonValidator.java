package org.json.builder.helper;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import org.json.builder.core.JsonBuilder;
import org.json.builder.helper.bean.InDirectValidation;
import org.json.builder.helper.bean.RuleBook;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonValidator {

    /**
     * <pre>
     * Compares the given JSON file against a set of validation rules specified in a rule book file.
     *
     * This method reads the rules defined in the `ruleBookFile`, which specifies JSONPath expressions
     * and validation criteria. It then applies these rules to the content of the `actualJsonFile` and
     * evaluates whether the values meet the specified conditions. The result is a list of error messages
     * for all validation failures. If all validations pass, the returned list will be empty.
     *
     * **RuleBook File Format:**
     * The `ruleBookFile` should be a JSON file containing validation rules. Each rule should specify:
     * - `pathExpression`: A JSONPath expression identifying the element(s) to validate.
     * - `condition`: The expected condition or value for the element(s).
     * - `description`: A brief description of the validation.
     *
     * For JSONPath expressions, refer to the [JSONPath documentation](https://github.com/json-path/JsonPath/blob/master/README.md).
     *
     * **Example Usage:**
     * Given:
     * 1. RuleBook file:
     * ```json
     * [
     *   {
     *     "description": "name node",
     *     "dirCheck": true,
     *     "dirValidation": {
     *       "expressions": [
     *         "$[?(@.name == 'Alice')]" // this is jsonpath expression
     *       ]
     *     },
     *     "indValidation": {
     *       "condition": "",
     *       "expressions": []
     *     }
     *   },
     *   {
     *     "description": "name1 node",
     *     "dirCheck": true,
     *     "dirValidation": {
     *       "expressions": [
     *         "$[?(@.name1 == 'Alice')]"
     *       ]
     *     },
     *     "indValidation": {
     *       "condition": "",
     *       "expressions": []
     *     }
     *   },
     *   {
     *     "description": "friends[].hobbies node",
     *     "dirCheck": false,
     *     "dirValidation": {
     *       "expressions": []
     *     },
     *     "indValidation": {
     *       "condition": "$.friends[?(@.age == 32)]", // if this condition is satisfied, then apply the below expressions on that particular node
     *       "expressions": [
     *         "$[*].hobbies[?(@ == 'gaming')]",
     *         "$[*].hobbies[?(@ == 'writting')]"
     *       ]
     *     }
     *   }
     * ]
     * ```
     * 2. Actual JSON file:
     * ```json
     * {
     *   "name": "Alice",
     *   "friends": [
     *     {
     *       "name": "Bob",
     *       "age": 28,
     *       "hobbies": [
     *         "gaming",
     *         "music"
     *       ]
     *     },
     *     {
     *       "name": "Charlie",
     *       "age": 32,
     *       "hobbies": [
     *         "paintting",
     *         "writting"
     *       ],
     *       "pets": [
     *         {
     *           "name": "Fluffy",
     *           "species": "cat"
     *         },
     *         {
     *           "name": "Buddy",
     *           "species": "dog"
     *         }
     *       ]
     *     }
     *   ]
     * }
     * ```
     * 3. Output in returned List<String> object
     * ```java
     * name1 node => $[?(@.name1 == 'Alice')]
     * friends[].hobbies node => $[*].hobbies[?(@ == 'gaming')]
     * ```
     * This method will return an empty list if there are no failures from validations.
     * </pre>
     *
     * @param ruleBookFile   the file path to the JSON containing validation rules
     * @param actualJsonFile the file path to the JSON to be validated
     * @return a list of error messages for failed validations, or an empty list if all validations pass
     * @see <a href="https://github.com/json-path/JsonPath/blob/master/README.md">Refer JSONPath Documentation to understand how to write jsonpath expressions.</a>
     */
    public static List<String> verify(String ruleBookFile, String actualJsonFile) {
        return verify(new File(ruleBookFile), new File(actualJsonFile));
    }

    public static List<String> verify(File ruleBookFile, File actualJsonFile) {

        // Read the actual JSON file to run rules against - for validation
        JsonBuilder actualJson_builder = JsonBuilder.objectBuilder().fromJsonFile(actualJsonFile).build();
        String actualJson = actualJson_builder.toPrettyString();

        // Read Rules from a rule-book.json file
        String json = JsonBuilder.arrayBuilder().fromJsonFile(ruleBookFile).buildAsJsonNode().toPrettyString();
        List<RuleBook> rules = JsonBuilder.transformJsonToPojoLst(json, RuleBook.class);

        // Set up configuration for JsonPath Expression
        Configuration configuration = Configuration.defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .addOptions(Option.ALWAYS_RETURN_LIST);

        // read the entire actual JSON and store as ReadContext by using the above configurations
        ReadContext context = JsonPath.using(configuration).parse(actualJson_builder.toPrettyString());

        // List of rules that failed
        List<String> failedRules = new ArrayList<>();

        // if the JSON to be validated is empty, then return an error message
        if (actualJson_builder.buildAsJsonNode().isEmpty()) {
            failedRules.add("Cannot verify an empty JSON");
            return failedRules;
        }

        for (RuleBook rule : rules) {
            if (rule.isDirCheck()) {
                List<String> jsonPathExpressions = rule.getDirValidation().getExpressions();

                for (String jsonPathExpression : jsonPathExpressions) {
                    List<Object> result = context.read(jsonPathExpression);

                    if (result.isEmpty()) // result's size > 0, then a match is found in actual JSON. Otherwise, failed.
                        failedRules.add(String.join(" => ", rule.getDescription(), jsonPathExpression));
                }
            } else {
                InDirectValidation inDirectValidation = rule.getIndValidation();
                String conditionPathExpression = inDirectValidation.getCondition();
                List<Object> result = context.read(conditionPathExpression);

                if (!result.isEmpty()) {
                    String satisfiedJsonNode = JsonBuilder.transformPojoToJsonNode(result).toPrettyString();

                    // if pathExpressions are given, then
                    if (!inDirectValidation.getExpressions().isEmpty()) {
                        ReadContext innerContext = JsonPath.using(configuration).parse(satisfiedJsonNode);

                        for (String innerPathExpression : inDirectValidation.getExpressions()) {
                            List<Object> innerResult = innerContext.read(innerPathExpression);
                            if (innerResult.isEmpty())
                                failedRules.add(String.join(" => ", rule.getDescription(), innerPathExpression));
                        }
                    }
                } else {
                    failedRules.add(rule.getDescription());
                }
            }
        }
        return failedRules;
    }
}
