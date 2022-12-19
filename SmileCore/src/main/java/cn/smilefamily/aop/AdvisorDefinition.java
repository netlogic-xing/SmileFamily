package cn.smilefamily.aop;

import cn.smilefamily.annotation.aop.*;
import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.BeanDefinitionBase;
import cn.smilefamily.common.MiscUtils;
import cn.smilefamily.util.BeanUtils;
import com.google.common.base.Strings;
import com.google.common.primitives.Primitives;
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

    public Object invokeAdvice(BeanDefinition bd, Object target, Object self, Method targetMethod,Method proceed, Object[] callArgs, Object result, Throwable e) {
        return MiscUtils.invoke(adviceMethod, aspectBeanDefinition.getBeanInstance(), constructArgs(bd, target, self, targetMethod,proceed, callArgs, result, e));
    }

    private void checkTypeMatch(Parameter p, Object arg, String message) {
        if (arg == null) {
            if(p.getType().isPrimitive()) {
                throw new AspectInjectException(p + " of " + adviceMethod + " cannot accept null");
            }
            return;
        }
        if (!p.getType().isAssignableFrom(Primitives.unwrap(arg.getClass()))) {
            throw new AspectInjectException(message);
        }
    }

    public Object[] constructArgs(BeanDefinition bd, Object target, Object self, Method targetMethod,Method proceed,  Object[] callArgs, Object result, Throwable e) {
        Parameter[] parameters = adviceMethod.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(Return.class)) {
                checkTypeMatch(param, result, param + " of " + adviceMethod + " cannot match the @Return type of " + result);
                arguments[i] = result;
                continue;
            }
            if (param.isAnnotationPresent(OrderValue.class)) {
                checkTypeMatch(param, order, param + " of " + adviceMethod + " cannot match the @OrderValue type int");
                arguments[i] = order;
                continue;
            }
            if (param.isAnnotationPresent(Throw.class)) {
                checkTypeMatch(param, e, param + " of " + adviceMethod + " cannot match the @Throw type of " + e);
                arguments[i] = e;
                continue;
            }
            if (param.isAnnotationPresent(BeanName.class)) {
                checkTypeMatch(param, bd.getName(), param + " of " + adviceMethod + " cannot match the @BeanName type " + String.class);
                arguments[i] = bd.getName();
                continue;
            }
            if (param.isAnnotationPresent(This.class)) {
                checkTypeMatch(param, self, param + " of " + adviceMethod + " cannot match the @This type " + self.getClass());
                arguments[i] = self;
                continue;
            }
            if (param.isAnnotationPresent(TargetObject.class)) {
                checkTypeMatch(param, target, param + " of " + adviceMethod + " cannot match the @TargetObject type " + target.getClass());
                arguments[i] = target;
                continue;
            }
            if (param.isAnnotationPresent(TargetMethod.class)) {
                checkTypeMatch(param, targetMethod, param + " of " + adviceMethod + " cannot match the @TargetMethod type " + targetMethod.getClass());
                arguments[i] = targetMethod;
                continue;
            }
            if (param.isAnnotationPresent(ProceedMethod.class)) {
                checkTypeMatch(param, proceed, param + " of " + adviceMethod + " cannot match the @TargetMethod type " + proceed.getClass());
                arguments[i] = proceed;
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
                        checkTypeMatch(param, callArgs[index], param + " of " + adviceMethod + " cannot match the " + arg + " of " + targetMethod);
                        arguments[i] = callArgs[index];
                        continue;
                    }
                }
                int index = arg.index() == -1 ? i : arg.index();
                if (index >= callArgs.length) {
                    throw new AspectInjectException("Arg(index=" + index + ") out of the bound of " + targetMethod.getName());
                }
                checkTypeMatch(param, callArgs[index], param + " of " + adviceMethod + " cannot match the " + arg + " of " + targetMethod);
                arguments[i] = callArgs[index];
                continue;
            }
            if (param.isNamePresent()) {
                int index = getArgIndex(targetParams, param.getName());
                if (index != -1) {
                    checkTypeMatch(param, callArgs[index], param + " of " + adviceMethod + " cannot match the argument " + index + " of " + targetMethod);
                    arguments[i] = callArgs[index];
                    continue;
                }
            }
            if (i >= callArgs.length) {
                throw new AspectInjectException("Arg(index=" + i + ") out of the bound of " + targetMethod);
            }
            checkTypeMatch(param, callArgs[i], param + " of " + adviceMethod + " cannot match the argument " + i + " of " + targetMethod);
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
    public boolean isPrefix(){
        return adviceType == AdviceType.BeforeAdvice || adviceType == AdviceType.AroundAdvice;
    }
    public boolean isAround(){
        return adviceType == AdviceType.AroundAdvice;
    }
    public boolean isSuffix(){
        return adviceType != AdviceType.BeforeAdvice;
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
        Order methodOrder = wrap(m).getAnnotation(Order.class);
        if (methodOrder != null) {
            this.order = methodOrder.value();
        } else {
            optionalOrder.ifPresent(order -> {
                this.order = order.value();
            });
        }
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
