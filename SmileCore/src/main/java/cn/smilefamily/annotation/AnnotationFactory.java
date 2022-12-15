package cn.smilefamily.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

@FunctionalInterface
public interface AnnotationFactory<A extends Annotation> {
    A getAnnotation(AnnotatedElement element);
}
