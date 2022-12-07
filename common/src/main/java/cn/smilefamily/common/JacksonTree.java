package cn.smilefamily.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonTree {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        //JsonNode node = mapper.createObjectNode();
        //JsonNode node = JsonNodeFactory.instance.objectNode();
        //JsonNode node = mapper.valueToTree(fromValue);
        //JsonNode node = mapper.convertValue(fromValue, JsonNode.class);
        NodeBean fromValue = new NodeBean(2016, "baeldung.com");
        Sub sub = new Sub("subbean");
        fromValue.setSub(sub);

        JsonNode node = mapper.valueToTree(fromValue);

        System.out.println(node);
        System.out.println(node.at("/sub/desc"));
    }
}
