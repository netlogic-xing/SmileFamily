package cn.smilefamily.annotation;


public class Main {
    public static void main(String[] args) {
        Object o = new Bean();
        System.out.println(o.getClass().isAnnotationPresent(Aliases.class));
        System.out.println(o.getClass().isAnnotationPresent(Alias.class));
        System.out.println(o.getClass().getAnnotation(Aliases.class));
        System.out.println(o.getClass().getAnnotation(Alias.class));
        System.out.println(o.getClass().getAnnotationsByType(Aliases.class)[0]);
        System.out.println(o.getClass().getAnnotationsByType(Alias.class)[0]);
    }
    @Alias("xxx")
    @Alias("yyy")
    private static class Bean{
        private String name;
    }
}
