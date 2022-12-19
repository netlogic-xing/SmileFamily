package cn.smilefamily.moduleexample.biz;

public class Car {
    private String name;
    private String vendor;

    public Car(String name, String vendor) {
        this.name = name;
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @Override
    public String toString() {
        return "Car{" +
                "name='" + name + '\'' +
                ", vendor='" + vendor + '\'' +
                '}';
    }
}
