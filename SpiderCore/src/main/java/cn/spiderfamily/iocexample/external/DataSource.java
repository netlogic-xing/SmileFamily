package cn.spiderfamily.iocexample.external;

import cn.spiderfamily.annotation.Value;

public class DataSource {
    private String name;
    @Value("spring.url")
    private String url;
    private Driver driver;

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public String getName() {
        return name;
    }

    public DataSource(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "name='" + name + '\'' +
                ", driver=" + driver + ", url=" + url +
                '}';
    }
}
