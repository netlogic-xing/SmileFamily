package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Value;

public class Driver {
    private String name;
    @Value("${connection.port:3308}")
    private int port;

    @Injected(name = "/order")
    private Order order;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "name='" + name + '\'' +
                ", port=" + port +
                ", order=" + order +
                '}';
    }

    public Driver(String name) {
        this.name = name;
    }
}
