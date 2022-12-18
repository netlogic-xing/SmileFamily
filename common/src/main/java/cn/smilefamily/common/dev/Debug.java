package cn.smilefamily.common.dev;

import cn.smilefamily.common.MiscUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.smilefamily.common.MiscUtils.shortName;

public class Debug {
    private static int deep = 0;
    public static boolean enabled = true;

    private static PrintStream debugOut;

    static {
        try {

            String logFileName = System.getProperty("smile.internal.dev.debug.file", "smile-internal-debug.log");
            debugOut = new PrintStream(new FileOutputStream(logFileName, true));
            debugOut.println("-".repeat(30) + new Date() + "-".repeat(30));
        } catch (FileNotFoundException e) {
            System.err.println("create internal");
        }
    }

    private record ParamPair(Parameter param, Object arg) {
    }

    public static String getTraceParams(Executable method, Object[] args) {

        return IntStream.range(0, args.length)
                .mapToObj(i -> new ParamPair(method.getParameters()[i], args[i]))
                .filter(p -> p.arg != null)
                .filter(p -> {
                    return !p.param.isAnnotationPresent(TraceParam.class) || p.param.getAnnotation(TraceParam.class).value();
                })
                .map(p -> {
                    if (p.arg instanceof Class clz) {
                        return shortName(clz.getName());
                    }
                    if (p.arg.getClass().isPrimitive() || args.getClass().isAssignableFrom(String.class)) {
                        return p.arg + "";
                    }
                    return getTraceInfo(p);
                }).collect(Collectors.joining(","));
    }

    private static String getTraceInfo(ParamPair p) {
        if (p.arg instanceof Collection<?> collection) {
            return collection.stream().map(o ->
                    Arrays.stream(o.getClass().getMethods()).filter(m -> m.isAnnotationPresent(TraceInfo.class))
                            .findFirst().map(m -> MiscUtils.invoke(m, o).toString()).orElse(o.toString())
            ).collect(Collectors.toList()).toString();
        }
        return Arrays.stream(p.arg.getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(TraceInfo.class))
                .findFirst().map(m -> MiscUtils.invoke(m, p.arg).toString())
                .orElse(p.arg.toString());
    }

    public static void debug(String content) {
        if (enabled) {
            debugOut.println(" ".repeat(4 * Debug.deep) + content);
        }
    }

    public static void enter(String content) {
        if (enabled) {
            debugOut.println(" ".repeat(4 * Debug.deep++) + content);
        }
    }

    public static void leave() {
        if (enabled) {
            deep--;
        }
    }
}
