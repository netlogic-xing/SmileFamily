package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.External;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.PostConstruct;
import cn.smilefamily.moduleexample.User;
import cn.smilefamily.moduleexample.common.Datasource;
import cn.smilefamily.moduleexample.portal.Menu;
import cn.smilefamily.moduleexample.portal.MenuManager;

import java.util.List;
public class CarService {

    @Injected
    @External("使用公共数据源")
    private Datasource datasource;

    public void add(Car car){
        System.out.println("add a car: " + car);
    }
    public void update(Car car){

    }

    public Car getCar(String name){
        return null;
    }

    public List<Car> getCars(){
        return null;
    }
    public void delete(Car car){
        return;
    }
}
