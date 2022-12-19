package cn.smilefamily.moduleexample.portal;

import cn.smilefamily.moduleexample.PermissionSubject;

public class Menu implements PermissionSubject {
    private String name;
    private String url;

    public Menu(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Menu{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String permissionToken() {
        return name;
    }
}
