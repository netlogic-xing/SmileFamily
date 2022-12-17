package cn.smilefamily.util;

import cn.smilefamily.BeanInitializationException;
import cn.smilefamily.annotation.core.External;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.Value;
import cn.smilefamily.bean.Dependency;
import cn.smilefamily.bean.ValueExtractors;
import cn.smilefamily.context.IllegalFileURLFormatException;
import cn.smilefamily.context.SimpleExpressionSyntaxException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;

public class BeanUtils {
    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
    }

    /**
     * 从方法或构造函数的参数及field获取bean名字
     *
     * @param p
     * @param defaultName
     * @return
     */
    public static String getBeanName(AnnotatedElement p, String defaultName) {
        String name = null;
        Injected injected = wrap(p).getAnnotation(Injected.class);
        if (injected != null && injected.value() != null && !injected.value().equals("")) {
            name = injected.value();
        }
        if (name == null) {
            name = defaultName;
        }
        return name;
    }

    public static String getActiveProfilePath(String fileURL, String activeProfile) {
        int pos = fileURL.lastIndexOf('.');
        if (pos == -1) {
            throw new IllegalFileURLFormatException("File URL must has extension name. " + fileURL);
        }
        return fileURL.substring(0, pos) + "-" + activeProfile + fileURL.substring(pos);
    }

    public static void traverse(JsonNode root, Deque<String> prefixes, BiConsumer<String, String> consumer) {
        if (root.isObject()) {
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = root.get(fieldName);
                prefixes.addLast(fieldName);
                traverse(fieldValue, prefixes, consumer);
            }
            prefixes.pollLast();
        } else if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            String last = prefixes.removeLast();
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                prefixes.addLast(last + "[" + i + "]");
                traverse(arrayElement, prefixes, consumer);
            }
        } else {
            consumer.accept(prefixes.stream().filter(p -> !p.equals("")).collect(Collectors.joining(".")), root.asText());
            prefixes.removeLast();
        }
    }

    public static void iterateYamlDocs(JsonParser parser, Consumer<JsonNode> docConsumer) {
        try {
            MappingIterator<JsonNode> list = mapper.readValues(parser, new TypeReference<>() {
            });
            for (MappingIterator<JsonNode> it = list; it.hasNext(); ) {
                JsonNode jsonNode = it.next();
                docConsumer.accept(jsonNode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<JsonParser> buildParser(String filename) {
        return FileUtils.getInputStream(filename).map(in -> {
            try {
                return mapper.createParser(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T> T toObject(JsonNode jsonNode, Type beanType) {
        try {
            return mapper.treeToValue(jsonNode, mapper.constructType(beanType));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeParser(JsonParser parser) {
        try {
            parser.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonParser buildExpressionSupportedParser(JsonParser parser, Function<String, String> valueProvider) {
        JsonParserDelegate delegate = new JsonParserDelegate(parser) {
            @Override
            public String getCurrentName() throws IOException {
                return valueProvider.apply(super.getCurrentName());
            }

            @Override
            public String getText() throws IOException {
                return valueProvider.apply(super.getText());
            }
        };
        return delegate;
    }

    public static Map<String, String> jsonTreeToProperties(JsonNode jsonNode) {
        Map<String, String> properties = new HashMap<>();
        if (jsonNode.isNull() || jsonNode.isTextual() || jsonNode.isArray()) {
            return properties;
        }
        ArrayDeque<String> prefixes = new ArrayDeque<>();
        prefixes.add("");
        traverse(jsonNode, prefixes, (key, value) -> {
            properties.put(key, value);
        });
        return properties;
    }

    public static String expression(String source, BiFunction<String, String, String> valueProvider) {
        return unescape(expression(source, false, source, valueProvider));
    }

    private static String expression(String source, boolean singleVariable, String original, BiFunction<String, String, String> valueProvider) {
        int tokenBegin = source.indexOf("${");
        if (tokenBegin == -1) {
            return singleVariable ? apply(source, valueProvider) : source;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(source, 0, tokenBegin);
        int tokenEnd = findMatched(source, original, '{', '}');
        if (tokenEnd == -1) {
            throw new SimpleExpressionSyntaxException("No end token } found in " + source);
        }
        String var = expression(source.substring(tokenBegin + 2, tokenEnd), true, original, valueProvider);
        if (var == null) {//如果表达式中部分因子为null，则整个表达式为null
            return null;
        }
        sb.append(var);
        String rest = expression(source.substring(tokenEnd + 1), false, original, valueProvider);
        if (rest == null) {
            return null;
        }
        sb.append(rest);
        return singleVariable ? apply(sb.toString(), valueProvider) : sb.toString();
    }

    private static String apply(String source, BiFunction<String, String, String> valueProvider) {
        String[] values = source.split("\\s*:\\s*", 2);
        String key = values[0];
        String defaultValue = values.length == 2 ? values[1] : null;
        return valueProvider.apply(key, defaultValue);
    }

    public static String unescape(String source) {
        if (source == null) {
            return null;
        }
        return source.replaceAll("\\\\([\\{\\}])", "$1");
    }

    private static int findMatched(String source, String original, char left, char right) {
        int deep = 0;
        char lastChar = '0';
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == left && lastChar != '\\') {
                deep++;
                continue;
            }
            if (c != right || lastChar == '\\') {
                continue;
            }

            deep--;
            if (deep == 0) {
                return i;
            }
            lastChar = c;
        }
        throw new SimpleExpressionSyntaxException("{ and } not matched in " + original);
    }

    public static void main(String[] args) {
        String source = "http://${${nameplate}_ip}:${${nameplate2}_port}/xx?id=$\\{id\\}";
        String source1 = "$\\{host\\}";
        System.out.println(source1);
        System.out.println(unescape(source1));
        Map<String, String> context = new HashMap<>();
        context.put("nameplate", "hostauto");
        context.put("nameplate2", "xx");
        String value = expression(source, (key, defautValue) -> {
            System.out.println("--" + key);
            return context.getOrDefault(key, key);
        });
        System.out.println(value);
        System.out.println(expression("${xingchenayang}_v", (key, defautValue) -> {
            return "xxy";
        }));
    }

    public static Object newInstance(Class<?> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    // The main function that checks if
// two given strings match. The pattern string
// may contain wildcard characters
    public static boolean match(String pattern, String input) {

        // If we reach at the end of both strings,
        // we are done
        if (pattern.length() == 0 && input.length() == 0)
            return true;
// Make sure to eliminate consecutive '*'
        if (pattern.length() > 1 && pattern.charAt(0) == '*') {
            int i = 0;
            while (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*')
                i++;
            pattern = pattern.substring(i);
        }

        // Make sure that the characters after '*'
        // are present in input string.
        // This function assumes that the pattern
        // string will not contain two consecutive '*'
        if (pattern.length() > 1 && pattern.charAt(0) == '*' &&
                input.length() == 0)
            return false;

        // If the pattern string contains '?',
        // or current characters of both strings match
        if ((pattern.length() > 1 && pattern.charAt(0) == '?') ||
                (pattern.length() != 0 && input.length() != 0 &&
                        pattern.charAt(0) == input.charAt(0)))
            return match(pattern.substring(1),
                    input.substring(1));

        // If there is *, then there are two possibilities
        // a) We consider current character of input string
        // b) We ignore current character of input string.
        if (pattern.length() > 0 && pattern.charAt(0) == '*')
            return match(pattern.substring(1), input) ||
                    match(pattern, input.substring(1));
        return false;
    }


// This code is contributed by
// sanjeev2552

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = BeanUtils.class.getClassLoader();
        }
        return classLoader.loadClass(className);
    }

    public static Object newInstance(Constructor constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    public static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    public static Object invokeStatic(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new BeanInitializationException(e);
        }
    }

    public static void setField(Field field, Object target, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new BeanInitializationException(e);
        }
    }

    //此方法可用Reflections改造
    public static Set<Class<?>> findAllAnnotatedClassIn(String packageName, Class<? extends Annotation>... annotationClasses) {
        Set<Class<?>> classes = new HashSet<>();
        Reflections reflections = new Reflections(packageName);
        return Arrays.stream(annotationClasses)
                .map(reflections::getTypesAnnotatedWith)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "."
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }

    /**
     * 获取方法或构造函数参数代表的依赖项
     * 方法参数和构造函数参数的autowired默认可以省略
     *
     * @param e
     * @return
     */
    public static List<Dependency> getParameterDeps(Executable e) {
        return Arrays.stream(e.getParameters()).map(p -> {
            External external = wrap(p).getAnnotation(External.class);
            String desc = external == null ? "" : external.value();
            if (wrap(p).isAnnotationPresent(Value.class)) {
                Value value = wrap(p).getAnnotation(Value.class);
                return new Dependency(value.value(), String.class, false, desc, external != null,
                        ValueExtractors.getValueExtractor(p.getType(), value)
                );
            }
            String beanName = getBeanName(p, p.getType().getName());
            if (wrap(p).isAnnotationPresent(Injected.class)) {
                return new Dependency(beanName, p.getParameterizedType(), wrap(p).getAnnotation(Injected.class).required(), desc, external != null);
            }
            return new Dependency(beanName, p.getParameterizedType(), desc, external != null);
        }).toList();
    }
}
