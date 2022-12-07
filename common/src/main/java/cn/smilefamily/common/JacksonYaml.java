package cn.smilefamily.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JacksonYaml {
    public static void main(String[] args) throws IOException {
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
        Order order = mapper.readValue(new File("common/src/main/resources/jack.yml"), Order.class);
        System.out.println(order);
    }
}
