package cn.smilefamily.aop;

import cn.smilefamily.annotation.aop.*;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.BeanDefinitionBase;
import cn.smilefamily.common.MiscUtils;
import cn.smilefamily.util.BeanUtils;
import com.google.common.base.Strings;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;
import static org.reflections.util.ReflectionUtilsPredicates.withAnnotation;

public class AdvisorDefinition implements Comparable<AdvisorDefinition> {


    @Override
    public int compareTo(AdvisorDefinition o) {
        return this.order - o.order;
    }

    public enum AdviceType {
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

    public Object invokeAdvice(BeanDefinition bd, Object target, Object self, Method targetMethod, Object[] callArgs, Object result, Throwable e) {
        return MiscUtils.invoke(adviceMethod, aspectBeanDefinition.getBeanInstance(), constructArgs(bd, target, self, targetMethod, callArgs, result, e));
    }

    public Object[] constructArgs(BeanDefinition bd, Object target, Object self, Method targetMethod, Object[] callArgs, Object result, Throwable e) {
        Parameter[] parameters = adviceMethod.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(Return.class)) {
                arguments[i] = result;
                continue;
            }
            if (param.isAnnotationPresent(Throw.class)) {
                arguments[i] = e;
            }
            if (param.isAnnotationPresent(BeanName.class)) {
                arguments[i] = bd.getName();
            }
            if (param.isAnnotationPresent(This.class)) {
                arguments[i] = self;
                continue;
            }
            if (param.isAnnotationPresent(TargetObject.class)) {
                arguments[i] = target;
                continue;
            }
            if (param.isAnnotationPresent(cn.smilefamily.annotation.aop.Method.class)) {
                arguments[i] = targetMethod;
                continue;
            }
            if (param.isAnnotationPresent(Args.class)) {
                arguments[i] = callArgs;
                continue;
            }
            Parameter[] targetParams = targetMethod.getParameters();
            if (param.isAnnotationPresent(Arg.class)) {
                Arg arg = wrap(param).getAnnotation(Arg.class);
                String argName = arg.name();
                if (Strings.isNullOrEmpty(argName)) {
                    argName = param.isNamePresent() ? param.getName() : null;
                }
                if (argName != null) {
                    int index = getArgIndex(targetParams, argName);
                    if (index != -1) {
                        arguments[i] = callArgs[index];
                        continue;
                    }
                }
                int index = arg.index() == -1 ? i : arg.index();
                if (index >= callArgs.length) {
                    throw new AspectInjectException("Arg(index=" + index + ") out of the bound of " + targetMethod.getName());
                }
                arguments[i] = callArgs[index];
                continue;
            }
            if (param.isNamePresent()) {
                int index = getArgIndex(targetParams, param.getName());
                if (index != -1) {
                    arguments[i] = callArgs[index];
                    continue;
                }
            }
            if (i >= callArgs.length) {
                throw new AspectInjectException("Arg(index=" + i + ") out of the bound of " + targetMethod.getName());
            }
            arguments[i] = callArgs[i];
        }
        return arguments;
    }

    private int getArgIndex(Parameter[] targetParams, String argName) {
        for (int j = 0; j < targetParams.length; j++) {
            if (targetParams[j].isNamePresent() && targetParams[j].getName().equals(argName)) {
                return j;
            }
        }
        return -1;
    }

    public AdviceType getAdviceType() {
        return adviceType;
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

    private BeanDefinition aspectBeanDefinition;

    public static List<AdvisorDefinition> buildFrom(Class<?> aspectClass, BeanDefinitionBase bd) {
        Optional<Order> optionalOrder = Optional.ofNullable(wrap(aspectClass).getAnnotation(Order.class));
        return Arrays.stream(AdviceType.values())
                .map(adviceType -> getAdvisorDefinitions(aspectClass, optionalOrder, adviceType, bd))
                .flatMap(Collection::stream)
                .toList();
    }

    private AdvisorDefinition(Optional<Order> optionalOrder, AdviceType adviceType, Method m, BeanDefinitionBase bd) {
        this.aspectBeanDefinition = bd;
        this.aspectName = bd.getName();
        this.adviceMethod = m;
        this.adviceType = adviceType;
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

    private static List<AdvisorDefinition> getAdvisorDefinitions(Class<?> aspectClass, Optional<Order> optionalOrder, AdviceType adviceType, BeanDefinitionBase bd) {
        return ReflectionUtils.getMethods(aspectClass, withAnnotation(adviceType.annotation)).stream()
                .map(m -> new AdvisorDefinition(optionalOrder, adviceType, m, bd))
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
        return selector.match(beanDefinition.getName(), beanDefinition.getType());
    }

    public boolean match(Method method) {
        return filter.include(method);
    }
}
