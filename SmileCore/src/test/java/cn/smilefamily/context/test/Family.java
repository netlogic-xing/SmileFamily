package cn.smilefamily.context.test;

import java.util.List;

public class Family {
    Person dad;
    Person mum;

    public Person getDad() {
        return dad;
    }

    public void setDad(Person dad) {
        this.dad = dad;
    }

    public Person getMum() {
        return mum;
    }

    public void setMum(Person mum) {
        this.mum = mum;
    }

    public List<Person> getChildren() {
        return children;
    }

    public void setChildren(List<Person> children) {
        this.children = children;
    }

    List<Person> children;
}
