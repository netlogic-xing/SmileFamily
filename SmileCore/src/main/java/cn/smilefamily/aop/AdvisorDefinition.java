package cn.smilefamily.aop;

import cn.smilefamily.annotation.aop.*;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.util.BeanUtils;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;
import static org.reflections.util.ReflectionUtilsPredicates.withAnnotation;

public class AdvisorDefinition implements Comparator<AdvisorDefinition> {
    @Override
    public int compare(AdvisorDefinition o1, AdvisorDefinition o2) {
        return o1.order - o2.order;
    }

    private enum AdviceType {
        BeforeAdvice(Before.class),
        AfterAdvice(After.class),
        AfterReturningAdvice(AfterReturning.class),
        AfterThrowingAdvice(AfterThrowing.class),
        AroundAdvice(Around.class);
        private Class<? extends Annotation> annotation;

        AdviceType(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }
    }
    private Method adviceMethod;
    private AdviceType adviceType;

    private String aspectName;
    private List<String> beanNameExpressions = new ArrayList<>();
    private List<Class<?>> beanClasses = new ArrayList<>();
    private List<Class<? extends Annotation>> annotationClasses = new ArrayList<>();

    private record MethodSelector(String nameExpression, Class<? extends Annotation> annClass) {
    }

    private List<MethodSelector> methodSelectors = new ArrayList<>();

    private int order = 0;
    private BeanSelector selector;
    private MethodFilter filter;

    public static List<AdvisorDefinition> buildFrom(Class<?> aspectClass) {
        Optional<Order> optionalOrder = Optional.ofNullable(wrap(aspectClass).getAnnotation(Order.class));
        return Arrays.stream(AdviceType.values())
                .map(adviceType -> getAdvisorDefinitions(aspectClass, optionalOrder, adviceType))
                .flatMap(Collection::stream)
                .toList();
    }

    private AdvisorDefinition(Optional<Order> optionalOrder, AdviceType adviceType, Method m) {
        this.adviceMethod = m;
        this.adviceType = adviceType;
        this.aspectName = m.getDeclaringClass().getName();
        optionalOrder.ifPresent(order -> {
            this.order = order.value();
        });
        Optional.ofNullable(m.getAnnotation(SelectBean.class)).ifPresentOrElse(a -> {
            this.selector = (BeanSelector) BeanUtils.newInstance(a.value());
        }, () -> {
            this.beanNameExpressions.addAll(getBeanNames(m));
            this.beanClasses.addAll(getBeanTypes(m));
            this.annotationClasses.addAll(getBeanAnnotations(m));
            this.selector = createSelector();
        });
        Optional.ofNullable(m.getAnnotation(FilterMethod.class)).ifPresentOrElse(a -> {
            this.filter = (MethodFilter) BeanUtils.newInstance(a.value());
        }, () -> {
            this.methodSelectors.addAll(getMethodSelectors(m));
            this.filter = createFilter();
        });
    }

    private MethodFilter createFilter() {
        return m -> {
            for (MethodSelector methodSelector : this.methodSelectors) {
                if (methodSelector.annClass != null && m.isAnnotationPresent(methodSelector.annClass)) {
                    return true;
                }
                if (BeanUtils.match(methodSelector.nameExpression, m.getName())) {
                    return true;
                }
            }
            return false;
        };
    }

    private BeanSelector createSelector() {
        return (name, type) -> {
            for (Class<?> beanClass : beanClasses) {
                if (beanClass.isAssignableFrom(type)) {
                    return true;
                }
            }
            for (Class<? extends Annotation> annotationClass : annotationClasses) {
                if (type.isAnnotationPresent(annotationClass)) {
                    return true;
                }
            }
            for (String nameExpression : beanNameExpressions) {
                if (BeanUtils.match(nameExpression, name)) {
                    return true;
                }
            }
            return false;
        };
    }

    private static List<AdvisorDefinition> getAdvisorDefinitions(Class<?> aspectClass, Optional<Order> optionalOrder, AdviceType adviceType) {
        return ReflectionUtils.getMethods(aspectClass, withAnnotation(adviceType.annotation)).stream()
                .map(m -> new AdvisorDefinition(optionalOrder, adviceType, m))
                .toList();
    }

    private static List<MethodSelector> getMethodSelectors(Method m) {
        return Arrays.stream(wrap(m).getAnnotationsByType(WithMethod.class)).map(a -> {
            return new MethodSelector(a.method(), a.annotation());
        }).toList();
    }

    private static List<? extends Class<? extends Annotation>> getBeanAnnotations(Method m) {
        return Arrays.stream(m.getAnnotationsByType(WithAnnotation.class))
                .map(a -> a.value())
                .toList();
    }

    private static List<? extends Class<?>> getBeanTypes(Method m) {
        return Arrays.stream(m.getAnnotationsByType(WithType.class)).map(WithType::value).toList();
    }

    private static List<String> getBeanNames(Method m) {
        return Arrays.stream(m.getAnnotationsByType(WithBean.class)).map(WithBean::value).toList();
    }

    public boolean accept(BeanDefinition beanDefinition) {
        return true;
    }

    public boolean match(Method method) {
        return true;
    }
}
