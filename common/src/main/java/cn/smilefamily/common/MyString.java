package cn.smilefamily.common;

public class MyString {
    private String value;

    public MyString(String value) {
        this.value = value;
    }

    public MyString() {
    }

    @Override
    public String toString() {
        return "MyString{" +
                "value='" + value + '\'' +
                '}';
    }
}
