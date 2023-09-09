/**
 * 
 */
package org.json.builder;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.SneakyThrows;


/**
 * @author rajkumarrajamani
 *
 */
public class JsonObjectBuilder implements JsonBuilders {

	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private Map<String, Object> jsonPathValueMapToAppend = new LinkedHashMap<>();
	private Map<String, Object> jsonPathValueMapToRemove = new LinkedHashMap<>();
	private ObjectNode rootObjectNode = MAPPER.createObjectNode();
	
	private JsonObjectBuilder() {
		
	}
	
	public static JsonObjectBuilder getInstance() {
		System.out.println("JsonObjectBuilder has been instantiated");
		return new JsonObjectBuilder();
	}
	
	@SneakyThrows
	public JsonObjectBuilder withExistingJsonFilePath(String jsonFileName) {
		if(jsonFileName.isBlank())
			throw new RuntimeException("Blank or Empty or Null value for JsonFileName. Please chekc json file name.");
		
		this.rootObjectNode = (ObjectNode) MAPPER.readTree(new File(jsonFileName));
		return this;
	}
	
	@SneakyThrows
	public JsonObjectBuilder withExistingJsonFile(File jsonFile) {
		if(!jsonFile.exists() || !jsonFile.isFile())
			throw new RuntimeException("Json File does not exist. Please chekc json file path.");
		
		this.rootObjectNode = (ObjectNode) MAPPER.readTree(jsonFile);
		return this;
	}
	
	@SneakyThrows
	public JsonObjectBuilder withExistingJsonString(String jsonString) {
		if(Objects.isNull(jsonString))
			throw new RuntimeException("Json String is NULL. Please check json string value.");
		
		if(jsonString.isBlank())
			throw new RuntimeException("Json String is either Blank or Null. Please check json string value.");
		
		this.rootObjectNode = (ObjectNode) MAPPER.readTree(jsonString);
		return this;
	}
	
	@SneakyThrows
	public JsonObjectBuilder withNewJsonObjectStyle() {
		String emotyJsonObjectStyle = "{}";
		this.rootObjectNode = (ObjectNode) MAPPER.readTree(emotyJsonObjectStyle);
		return this;
	}
	
	public String build() {
		if(Objects.nonNull(jsonPathValueMapToAppend)) {
			for(Entry<String, Object> node : jsonPathValueMapToAppend.entrySet()) {
				setJsonPointerValueInJsonObject(rootObjectNode, JsonPointer.compile(node.getKey()), (JsonNode) node.getValue());
			}
		}
		
		jsonPathValueMapToAppend = new HashMap<>();
		
		if(Objects.nonNull(jsonPathValueMapToRemove)) {
			for(Entry<String, Object> node : jsonPathValueMapToRemove.entrySet()) {
				removeJsonPointerValueInJsonObject(rootObjectNode, JsonPointer.compile(node.getKey()));
			}
		}
		
		jsonPathValueMapToRemove = new HashMap<>();
		
		return rootObjectNode.toString();
	}
	
	public boolean isEmpty() {
		return rootObjectNode.isNull() || rootObjectNode.isEmpty() || rootObjectNode.isMissingNode();
	}
	
	public JsonNode buildAsJsonNode() {
		this.build();
		if(Objects.isNull(rootObjectNode))
			throw new RuntimeException("JsonObjectBuilder is not fed with required JSON file. Please add JSON first.");
		return rootObjectNode;
	}
	
	public JsonObjectBuilder appendJsonWithValue(String jsonNodePath, Object value, String dataTypeOfValue) {
		if(isSkippable(value)) {
			jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
			jsonPathValueMapToAppend.put(jsonNodePath, convertValueOfRequiredDataType(value, dataTypeOfValue));
		}
		return this;
	}
	
	public JsonObjectBuilder appendJsonWithValue(String jsonNodePath, Object value) {
		if(isSkippable(value)) {
			jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
			jsonPathValueMapToAppend.put(jsonNodePath, convertValueOfRequiredDataType(value, "Text"));
		}
		return this;
	}
	
	public JsonObjectBuilder removeJsonNode(String jsonNodePath) {
		jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
		jsonPathValueMapToRemove.put(jsonNodePath, "");
		return this;
	}
	
	public JsonNode getValueAtNode(String jsonNodePath) {
		jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
		return rootObjectNode.at(jsonNodePath);
	}
	
	public JsonNode getValueAtNode(JsonNode node, String jsonNodePath) {
		jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
		return node.at(jsonNodePath);
	}
	
	public static String getValueAtNodeAsText(JsonNode node, String jsonNodePath) {
		jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
		
		if(node.at(jsonNodePath).isMissingNode())
			return "null";
		
		return node.at(jsonNodePath).asText();
	}
	
	public JsonNode asJsonNode() {
		if(Objects.isNull(rootObjectNode))
			throw new RuntimeException("JsonBuilder is not fed with required JSON file. Please add JSON first.");
		return rootObjectNode;
	}
	
	public void clearBuilder() {
		rootObjectNode = MAPPER.createObjectNode();
		jsonPathValueMapToAppend = new LinkedHashMap<>();
		jsonPathValueMapToRemove = new LinkedHashMap<>();
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	public <T> T extractJsonIntoPojo(Class<?> classType) {
		ObjectMapper oMapper = new ObjectMapper();
		return (T) oMapper.treeToValue(rootObjectNode, classType);
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	public <T> T extractJsonAtNodeAndConvertIntoPojo(String jsonNodePath, Class<?> classType) {
		ObjectMapper oMapper = new ObjectMapper();
		jsonNodePath = convertJsonNodePathWithSlashSeparator(jsonNodePath);
		return (T) oMapper.treeToValue(rootObjectNode.at(jsonNodePath), classType);
	}

	private boolean isSkippable(Object value) {
		if(Objects.nonNull(value)) {
			String stringValue = String.valueOf(value);
			return stringValue.equalsIgnoreCase(NodeValueType.SKIP.getType()) || stringValue.equalsIgnoreCase(NodeValueType.IGNORE.getType());
		}

		return false;
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	public static <T> T convertJsonStringIntoPojo(String jsonString, Class<?> classType) {
		ObjectMapper oMapper = new ObjectMapper();
		oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ObjectNode node = (ObjectNode) oMapper.readTree(jsonString);
		return (T) oMapper.treeToValue(node, classType);
	}
	
	public static JsonNode convertPojoToJsonNode (Object object) {
		return MAPPER.valueToTree(object);
	}
	
	public static String convertPojoToPretryJsonString (Object object) {
		return generateString(object, true);
	}
	
	public static String convertPojoToJsonString (Object object) {
		return generateString(object, false);
	}
	
	private static String generateString (Object object, boolean pretty) {
		if(pretty)
			return MAPPER.valueToTree(object).toPrettyString();
		else
			return MAPPER.valueToTree(object).toString();
	}
	
	private static String convertJsonNodePathWithSlashSeparator(String jsonNodePath) {
		if(Objects.nonNull(jsonNodePath)) {
			jsonNodePath = StringUtils.replace(jsonNodePath, ".", "/");
			jsonNodePath = StringUtils.replace(jsonNodePath, "[", "/");
			jsonNodePath = StringUtils.replace(jsonNodePath, "]", "/");
			jsonNodePath = StringUtils.replace(jsonNodePath, "//", "/");
			jsonNodePath = StringUtils.prependIfMissing(jsonNodePath, "/");
			jsonNodePath = StringUtils.removeEnd(jsonNodePath, "/");
		}
		return jsonNodePath;
	}
	
	private void setJsonPointerValueInJsonObject(ObjectNode node, JsonPointer pointer, JsonNode value) {
		JsonPointer parentPointer = pointer.head();
		JsonNode parentNode = node.at(parentPointer);
		String fieldName = pointer.last().toString().substring(1);
		
		if(parentNode.isMissingNode() || parentNode.isNull()) {
			parentNode = StringUtils.isNumeric(fieldName) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
			setJsonPointerValueInJsonObject(node, parentPointer, parentNode); // recursively reconstruct hierarchy
		}
		
		if(parentNode.isArray()) {
			ArrayNode arrayNode = (ArrayNode) parentNode;
			int index = Integer.parseInt(fieldName);
			// Expand array in case index is greater than array size
			for (int i = arrayNode.size() ; i <= index ; i ++) {
				arrayNode.addObject();
			}
			arrayNode.set(index, value);
		} else if(parentNode.isObject()) {
			( (ObjectNode) parentNode).set(fieldName, value);
		} else {
			throw new IllegalArgumentException("'" + fieldName + "' can't be set for parent node '" + parentPointer 
					+ "' because parent is not a container but " + parentNode.getNodeType().name());
		}
	}
	
	private void removeJsonPointerValueInJsonObject(ObjectNode node, JsonPointer pointer) {
		JsonPointer parentPointer = pointer.head();
		JsonNode parentNode = node.at(parentPointer);
		String fieldName = pointer.last().toString().substring(1);
		
		if(parentNode.isMissingNode() || parentNode.isNull()) {
			return;
		}
		
		if(parentNode.isArray()) {
			ArrayNode arrayNode = (ArrayNode) parentNode;
			int index = Integer.parseInt(fieldName);
			arrayNode.remove(index);
		} else if(parentNode.isObject()) {
			( (ObjectNode) parentNode).remove(fieldName);
		} else {
			throw new IllegalArgumentException("'" + fieldName + "' can't be set for parent node '" + parentPointer 
					+ "' because parent is not a container but " + parentNode.getNodeType().name());
		}
	}
	
	private JsonNode convertValueOfRequiredDataType(Object value, String valueType) {
		
		String stringValue = "";
		
		if(Objects.nonNull(value))
			stringValue = String.valueOf(value);
		
		if(
				valueType.equalsIgnoreCase(NodeValueType.INT.getType()) ||
				valueType.equalsIgnoreCase(NodeValueType.NUMBER.getType()) ||
				valueType.equalsIgnoreCase(NodeValueType.INTEGER.getType()) 
				)
			return new IntNode(Integer.parseInt(stringValue));
		else if (valueType.equalsIgnoreCase(NodeValueType.LONG.getType()))
			return new LongNode(Long.parseLong(stringValue));
		else if (
				valueType.equalsIgnoreCase(NodeValueType.DOUBLE.getType()) ||
				valueType.equalsIgnoreCase(NodeValueType.DECIMAL.getType()) ||
				valueType.equalsIgnoreCase(NodeValueType.FLOAT.getType())
				)
			return new DoubleNode(Double.parseDouble(stringValue));
		else if (valueType.equalsIgnoreCase(NodeValueType.BOOLEAN.getType()))
			return BooleanNode.valueOf(Boolean.parseBoolean(stringValue));
		else if (
				valueType.equalsIgnoreCase(NodeValueType.STRING.getType()) ||
				valueType.equalsIgnoreCase(NodeValueType.TEXT.getType())
				) {
			if(Objects.isNull(value))
				return null;
			else if (
					String.valueOf(value).equalsIgnoreCase(NodeValueType.BLANK.getType()) ||
					String.valueOf(value).equalsIgnoreCase(NodeValueType.EMPTY.getType())
					) 
				return new TextNode("");
			else
				return new TextNode(stringValue);
		} else if (valueType.equalsIgnoreCase(NodeValueType.NULL.getType()))
			return null;
		else if (valueType.equalsIgnoreCase(NodeValueType.OBJECTNODE.getType()))
			return new ObjectMapper().createObjectNode();
		else if (valueType.equalsIgnoreCase(NodeValueType.ARRAYNODE.getType()))
			return new ObjectMapper().createArrayNode();
		else if (valueType.equalsIgnoreCase(NodeValueType.OBJECT_STYLE_JSON.getType())) {
			if(!stringValue.equalsIgnoreCase(""))
				return JsonObjectBuilder.getInstance().withExistingJsonString(stringValue).asJsonNode();
			else
				return new ObjectMapper().createObjectNode();
		} else if (valueType.equalsIgnoreCase(NodeValueType.ARRAY_STYLE_JSON.getType())) {
			if(!stringValue.equalsIgnoreCase(""))
				return null;//JsonArrayBuilder.getInstance().withExistingJsonString(stringValue).asJsonNode();
			else
				return new ObjectMapper().createArrayNode();
		} else
			return new TextNode(stringValue);
	}
	
	@Getter
	public enum NodeValueType {
		NUMBER("Number"),
		INT("Int"),
		INTEGER("Integer"),
		LONG("Long"),
		FLOAT("Float"),
		DECIMAL("Decimal"),
		DOUBLE("Double"),
		TEXT("Text"),
		STRING("String"),
		BOOLEAN("Boolean"),
		NULL("Null"),
		BLANK("Blank"),
		EMPTY("Empty"),
		OBJECTNODE("ObjectNode"),
		ARRAYNODE("ArrayNode"),
		OBJECT_STYLE_JSON("JsonObject"),
		ARRAY_STYLE_JSON("JsonArray"),
		SKIP("Skip"),
		IGNORE("Ignore");
		
		private final String type;
		
		NodeValueType(String type) {
			this.type = type;
		}

	}
	
}
