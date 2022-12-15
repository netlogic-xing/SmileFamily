package cn.smilefamily.extension;

public interface Extension {
    public String name();
    public void load();

    default void unload() {

    }
}
