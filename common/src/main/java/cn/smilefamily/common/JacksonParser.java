package cn.smilefamily.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class JacksonParser {
    public static void main(String[] args) throws IOException {
        File file = new File("common/src/main/resources/jack.yml");
        YAMLFactory factory = new YAMLFactory();
        JsonParser parser  = factory.createParser(file);
        while(!parser.isClosed()){
            JsonToken jsonToken = parser.nextToken();

            System.out.println("jsonToken = " + jsonToken+"->" + parser.getText());
        }
    }
}
