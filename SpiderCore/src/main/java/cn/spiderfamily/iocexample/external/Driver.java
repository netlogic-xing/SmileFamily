package cn.spiderfamily.iocexample.external;

import cn.spiderfamily.annotation.Value;
public class Driver {
    private String name;
    @Value(value = "spring.server.port", defaultValue = "1521")
    private int port;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "name='" + name + '\'' +
                "port=" + port +
                '}';
    }

    public Driver(String name) {
        this.name = name;
    }
}
