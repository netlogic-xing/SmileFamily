package cn.smilefamily.common;

public class NodeBean {
    private int id;
    private String name;

    private Sub sub;

    public void setSub(Sub sub) {
        this.sub = sub;
    }

    public NodeBean() {
    }

    public NodeBean(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // standard getters and setters
}