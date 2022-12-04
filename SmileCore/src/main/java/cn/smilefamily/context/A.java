package cn.smilefamily.context;

public class A {
    int x = 0;
    private String name;
    private boolean enabled;
    private int age;

    public A(String name) {
        this.name = name;
    }

    public A() {
    }

    void show(){
        System.out.println("name=" + name + ", enabled=" + enabled + ", age=" + age);
    }

    String getName() {
        return name;
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    int getAge() {
        return age;
    }

    void setAge(int age) {
        this.age = age;
    }
}
