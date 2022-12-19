package cn.smilefamily.annotation;

import cn.smilefamily.util.BeanUtils;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

public class EnhancedAnnotatedElement implements AnnotatedElement {
    private AnnotatedElement target;

    public static EnhancedAnnotatedElement wrap(AnnotatedElement target) {
        return new EnhancedAnnotatedElement(target);
    }

    public EnhancedAnnotatedElement(AnnotatedElement target) {
        this.target = target;
    }

    private static <A extends Annotation> A proxy(Class<A> clazz, Object target) {
        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(new Class[]{clazz, Annotation.class});
        //factory.writeDirectory="./code";
        //factory.setFilter(m->m.isAnnotationPresent(AliasFor.class));
        Object proxy = BeanUtils.newInstance(factory.createClass());
        ((Proxy) proxy).setHandler((self, m, proceed, args) -> {
            if (!m.isAnnotationPresent(AliasFor.class)) {
                return m.invoke(target, args);
            }
            Object result = m.invoke(target, args);
            if (result != null && !result.equals("")) {
                return result;
            }
            AliasFor alias = m.getAnnotation(AliasFor.class);
            return target.getClass().getDeclaredMethod(alias.value()).invoke(target, args);
        });
        return (A) proxy;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        A a = target.getAnnotation(annotationClass);
        if (a != null) {
            return proxy(annotationClass, a);
        }
        for (Class<? extends Annotation> aliasAnnotation : AnnotationRegistry.getAliasAnnotations(annotationClass)) {
            a = (A) AnnotationRegistry.getFactory(aliasAnnotation).getAnnotation(target);
            if (a != null) {
                return proxy(annotationClass, a);
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return Arrays.stream(target.getAnnotations()).map(a -> proxy(a.annotationType(), a)).toList().toArray(new Annotation[0]);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return Arrays.stream(target.getDeclaredAnnotations()).map(a -> proxy(a.annotationType(), a)).toList().toArray(new Annotation[0]);
    }
}
