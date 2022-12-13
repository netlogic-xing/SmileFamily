package cn.smilefamily.util;

import com.google.common.collect.Maps;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class FileUtils {
    public static String extensionName(String fileURL){
        int pos = fileURL.lastIndexOf('.');
        if(pos == -1){
            return null;
        }
        return fileURL.substring(pos);
    }
    public static Optional<InputStream> getInputStream(String fileURL) {
        try {
            if (fileURL.startsWith("classpath:")) {
                String path = fileURL.substring("classpath:".length());
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    classLoader = FileUtils.class.getClassLoader();
                }
                return Optional.ofNullable(classLoader.getResourceAsStream(path));
            } else if (fileURL.startsWith("file:")) {
                String path = fileURL.substring("file:".length());
                return Optional.ofNullable(new FileInputStream(path));
            }
            throw new UnsupportedOperationException("Unsupported protocol: " + fileURL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        getInputStream("classpath:application.yml");
        //getInputStream("classpath:test1.yml");
        getInputStream("classpath:cn/smilefamily/context/test1.yml");
    }

    public static Optional<Map<String, String>> propertiesFrom(String fileURL) {
        Properties properties = new Properties();
        return getInputStream(fileURL).map(inputStream -> {
            try {
                properties.load(inputStream);
                return Maps.fromProperties(properties);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
