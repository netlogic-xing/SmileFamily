package cn.smilefamily.common;

import org.reflections.ReflectionUtils;
import org.reflections.util.ReflectionUtilsPredicates;

public class TestBean1 {

    private String name;
    public void show(){
        System.out.println("test..");
    }

    public static void main(String[] args) {
        ReflectionUtils.getMethods(TestBean1.class, ReflectionUtilsPredicates.withName("show")).forEach(m->{
           MiscUtils.invoke(m,new String());
        });
    }
}
