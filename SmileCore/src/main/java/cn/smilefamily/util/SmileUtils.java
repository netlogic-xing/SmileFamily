package cn.smilefamily.util;

import cn.smilefamily.bean.BeanDefinition;
import cn.smilefamily.bean.Dependency;
import cn.smilefamily.bean.BeanDefinitionBase;
import cn.smilefamily.context.BeanContext;

import java.util.Comparator;

public class SmileUtils {
    private record Dep(Class<?> clazz, String name, String context, String description, boolean external) {
    }

    public static void inspectConfig(Class<?> config) {
        BeanContext beanContext = new BeanContext(config);
        System.out.println("========================Analyze Config class " + config.getName() + "[" + beanContext.getName() + "]" + "==".repeat(20) + "\n");
        beanContext.getBeanDefinitions().stream().sorted(Comparator.comparing(BeanDefinition::getSource)).forEach(bd -> {
            System.out.println(bd);
        });
        System.out.println("\n" + "-".repeat(45) + "exported beans" + "-".repeat(45) + "\n");
        beanContext.export().stream().forEach(bd -> {
            System.out.println(bd.getName() + "\n\t" + bd.getDescription());
        });
        System.out.println("\n" + "-".repeat(45) + "external dependencies" + "-".repeat(45) + "\n");
        beanContext.getBeanDefinitions().stream().sorted(Comparator.comparing(BeanDefinition::getSource))
                .filter(bd -> bd instanceof BeanDefinitionBase)
                .map(bd -> (BeanDefinitionBase) bd)
                .filter(bd -> bd.getDependencies().stream().anyMatch(Dependency::external)).forEach(bd -> {
                    bd.getDependencies().stream().filter(Dependency::external).forEach(dep -> {
                        System.out.println("Bean(" + bd.getName() + ")->" + dep.name() + "\n\t" + dep.description());
                    });
                });

        System.out.println("\n" + "=".repeat(45) + " end " + "=".repeat(45));
    }
}
