package cn.smilefamily.annotation;


import cn.smilefamily.annotation.core.Scope;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.Function;

public class AnnotationRegistry {
    private static Multimap<Class<? extends Annotation>, Class<? extends Annotation>> registry = LinkedHashMultimap.create();
    private static Map<Class<? extends Annotation>, AnnotationFactory> factories = new HashMap<>();

    static {

    }

    public static void register(Class<? extends Annotation> annotation,
                                Class<? extends Annotation> aliasAnnotation,
                                Function<AnnotatedElement, Annotation> factory) {
        registry.put(annotation, aliasAnnotation);
        factories.put(aliasAnnotation, element -> element.isAnnotationPresent(aliasAnnotation) ? factory.apply(element) : null);
    }

    public static AnnotationFactory getFactory(Class<? extends Annotation> annotationClass) {
        return factories.get(annotationClass);
    }

    public static Collection<Class<? extends Annotation>> getAliasAnnotations(Class<? extends Annotation> source) {
        return registry.get(source);
    }

    public static Class<? extends Annotation>[] getSynonymousAnnotations(Class<? extends Annotation> source) {
        List<Class<? extends Annotation>> list = new ArrayList<>();
        list.addAll(registry.get(source));
        list.add(source);
        return list.toArray(new Class[0]);
    }

    public static void main(String[] args) {
        System.out.println(registry);
        System.out.println(getAliasAnnotations(Scope.class));
    }
}
