package cn.smilefamily.aop;

import cn.smilefamily.bean.BeanDefinition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ComposedAdvisor {
    private List<AdvisorDefinition> advisorDefinitions = new ArrayList<>();
    private static Method executeMethod;

    static {
        executeMethod = Arrays.stream(ComposedAdvisor.class.getDeclaredMethods()).filter(m -> m.getName().equals("executeInternal")).findFirst().get();
    }

    public boolean isEmpty() {
        return advisorDefinitions.isEmpty();
    }

    public ComposedAdvisor(List<AdvisorDefinition> advisorDefinitions) {
        this.advisorDefinitions.addAll(advisorDefinitions);
        Collections.sort(this.advisorDefinitions);
    }

    private List<AdvisorDefinition> getClosedAdvisorDefinitions(List<AdvisorDefinition> advisorDefinitions) {
        List<AdvisorDefinition> list = new ArrayList<>();
        for (int i = 0; i < advisorDefinitions.size(); i++) {
            if (advisorDefinitions.get(i).isAround()) {

                list.addAll(advisorDefinitions.subList(0, i));
                advisorDefinitions.removeAll(list);
                return list;
            }
        }
        list.addAll(advisorDefinitions);
        advisorDefinitions.clear();
        return list;
    }


    public Object executeInternal(BeanDefinition bd, Object target, Object self, Method originalProceed, Method targetMethod, Object[] originalArgs, Object result, Throwable e,
                                  List<AdvisorDefinition> beforeAndAroundList, List<AdvisorDefinition> afterAndAroundList) throws Throwable {
        List<AdvisorDefinition> prefixes = getClosedAdvisorDefinitions(beforeAndAroundList);
        List<AdvisorDefinition> suffixes = getClosedAdvisorDefinitions(afterAndAroundList);
        for (AdvisorDefinition prefix : prefixes) {
            prefix.invokeAdvice(bd, target, self, targetMethod, originalProceed, originalArgs, null, null);
        }
        Object ret = null;
        try {
            if (beforeAndAroundList.isEmpty()) {
                ret = targetMethod.invoke(target, originalArgs);
            } else {
                AdvisorDefinition aroundAdvisor = beforeAndAroundList.remove(0);
                afterAndAroundList.remove(0);
                ret = aroundAdvisor.invokeAdvice(bd, this, self, executeMethod,originalProceed, new Object[]{bd, target, self, originalProceed, targetMethod, originalArgs, null, null, beforeAndAroundList, afterAndAroundList}, null, null);
            }
            List<AdvisorDefinition> list = suffixes.stream().filter(a -> a.getAdviceType() != AdvisorDefinition.AdviceType.AfterThrowingAdvice).toList();
            for (int i = list.size() - 1; i >= 0; i--) {
                AdvisorDefinition suffix = list.get(i);
                suffix.invokeAdvice(bd, target, self, targetMethod,originalProceed, originalArgs, ret, null);
            }
        } catch (Throwable t) {
            List<AdvisorDefinition> list = suffixes.stream().filter(a -> a.getAdviceType() == AdvisorDefinition.AdviceType.AfterThrowingAdvice).toList();
            for (int i = list.size() - 1; i >= 0; i--) {
                AdvisorDefinition suffix = list.get(i);
                suffix.invokeAdvice(bd, target, self, targetMethod,originalProceed, originalArgs, null, t);
            }
            return ret;
        }
        return ret;
    }

    public Object execute(BeanDefinition bd, Object target, Object self, Method originalProceed, Method targetMethod, Object[] originalArgs) throws Throwable {
        List<AdvisorDefinition> beforeAndAroundList = new ArrayList<>();
        beforeAndAroundList.addAll(advisorDefinitions.stream().filter(a -> a.match(targetMethod)).filter(a -> a.isPrefix()).toList());
        List<AdvisorDefinition> afterAndAroundList = new ArrayList<>();
        afterAndAroundList.addAll(advisorDefinitions.stream().filter(a -> a.match(targetMethod)).filter(a -> a.isSuffix()).toList());
//        advisorDefinitions.stream().filter(a -> a.match(targetMethod)).filter(a -> a.isSuffix()).forEach(a->{
//            afterAndAroundList.add(0, a);
//        });
        return executeInternal(bd, target, self, originalProceed, targetMethod, originalArgs, null, null, beforeAndAroundList, afterAndAroundList);
    }

    public boolean match(Method m) {
        return advisorDefinitions.stream().anyMatch(a -> a.match(m));
    }

//    public Object execute(BeanDefinition bd, Object target, Object self, TargetMethod originalProceed, TargetMethod targetMethod, Object[] originalArgs) throws Throwable {
//        AdvisorDefinition targetAdvisorDefinition = null;
//        for (AdvisorDefinition advisorDefinition : advisorDefinitions) {
//            if (advisorDefinition.match(targetMethod)) {
//                if (advisorDefinition.getAdviceType() == AdvisorDefinition.AdviceType.BeforeAdvice) {
//                    advisorDefinition.invokeAdvice(bd, target, self, targetMethod, originalArgs, null, null);
//                    continue;
//                }
//                if (advisorDefinition.getAdviceType() == AdvisorDefinition.AdviceType.AroundAdvice) {
//                    targetAdvisorDefinition = advisorDefinition;
//                    break;
//                }
//            }
//        }
//        Object result = null;
//        try {
//            if (targetAdvisorDefinition != null) {
//                result = targetAdvisorDefinition.invokeAdvice(bd, target, self, targetMethod, originalArgs, null, null);
//            } else {
//                result = targetMethod.invoke(target, originalArgs);
//            }
//            for (int i = advisorDefinitions.size() - 1; i >= 0; i--) {
//                AdvisorDefinition advisorDefinition = advisorDefinitions.get(i);
//                if (advisorDefinition.match(targetMethod)) {
//                    if (advisorDefinition.getAdviceType() == AdvisorDefinition.AdviceType.AfterAdvice
//                            || advisorDefinition.getAdviceType() == AdvisorDefinition.AdviceType.AfterReturningAdvice) {
//                        advisorDefinition.invokeAdvice(bd, target, self, targetMethod, originalArgs, result, null);
//                    }
//                }
//            }
//        } catch (Throwable t) {
//            for (int i = advisorDefinitions.size() - 1; i >= 0; i--) {
//                AdvisorDefinition advisorDefinition = advisorDefinitions.get(i);
//                if (advisorDefinition.match(targetMethod)) {
//                    if (advisorDefinition.getAdviceType() == AdvisorDefinition.AdviceType.AfterAdvice
//                            || advisorDefinition.getAdviceType() == AdvisorDefinition.AdviceType.AfterThrowingAdvice) {
//                        advisorDefinition.invokeAdvice(bd, target, self, targetMethod, originalArgs, result, t);
//                    }
//                }
//            }
//        }
//        return result;
//    }
}
