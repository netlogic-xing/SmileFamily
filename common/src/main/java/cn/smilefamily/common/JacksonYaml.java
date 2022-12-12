package cn.smilefamily.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class JacksonYaml {
    public static void main(String[] args) throws IOException {
        test();
    }

    private static void test3() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        JsonNode root = mapper.readTree(new File("common/src/main/resources/orderOutput.yaml"));
        Order order = mapper.treeToValue(root, Order.class);
        System.out.println(order);
    }

    private static void test2() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        List<OrderLine> lines = new ArrayList<>();
        lines.add(new OrderLine("Copper Wire (200ft)", 1,
                new BigDecimal(50.67).setScale(2, RoundingMode.HALF_UP)));
        lines.add(new OrderLine("Washers (1/4\")", 24,
                new BigDecimal(.15).setScale(2, RoundingMode.HALF_UP)));
        Order order = new Order(
                "B-9910",
                LocalDate.parse("2019-04-18", DateTimeFormatter.ISO_DATE),
                "Customer, Jane",
                lines);
        mapper.writeValue(new File("common/src/main/resources/orderOutput.yaml"), order);
    }

    private static void test() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        File file = new File("common/src/main/resources/jack.yml");
        JsonParser parser = mapper.createParser(file);
        JsonParserDelegate delegate = new JsonParserDelegate(parser){
            @Override
            public String getCurrentName() throws IOException {
                return "+p+s#"+super.getCurrentName();
            }
            @Override
            public String getText() throws IOException {
                return "+p+s"+super.getText();
            }
        };

        MappingIterator<JsonNode> list = mapper.readValues(delegate, new TypeReference<>() {
        });
        for (MappingIterator<JsonNode> it = list; it.hasNext(); ) {
            JsonNode jsonNode = it.next();
            System.out.println("-".repeat(40));
            if (jsonNode.isNull() || jsonNode.isTextual()||jsonNode.isArray()) {
                continue;
            }
            ArrayDeque<String> prefixes = new ArrayDeque<>();
            prefixes.add("");
            traverse(jsonNode, prefixes);
        }
        parser.close();
    }

    public static void traverse(JsonNode root, Deque<String> prefixes) {
        if (root.isObject()) {
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = root.get(fieldName);
                prefixes.addLast(fieldName);
                traverse(fieldValue, prefixes);
            }
            prefixes.pollLast();
        } else if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            String last = prefixes.removeLast();
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                prefixes.addLast(last + "[" + i + "]");
                traverse(arrayElement, prefixes);
            }
        } else {
            System.out.println(prefixes.stream().filter(p->!p.equals("")).collect(Collectors.joining(".")) + "=" + root.asText());
            prefixes.removeLast();
        }
    }
}
