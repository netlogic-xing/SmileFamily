package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.annotation.aop.Aspect;
import cn.smilefamily.annotation.aop.Before;
import cn.smilefamily.annotation.aop.WithMethod;
import cn.smilefamily.annotation.aop.WithType;
import cn.smilefamily.annotation.core.External;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.PostConstruct;
import cn.smilefamily.moduleexample.User;
import cn.smilefamily.moduleexample.portal.Menu;
import cn.smilefamily.moduleexample.portal.MenuManager;

@Aspect
public class PermissionAspect {
    private User current = new User("xl");
    @Injected
    @External("依赖外界提供菜单管理器")
    private MenuManager menuManager;
    @PostConstruct
    public void registerMenu(){
        menuManager.registerMenu(new Menu("xl"));
        menuManager.registerMenu(new Menu("xr"));
        menuManager.registerMenu(new Menu("xfamily"));
    }
    @Before
    @WithType(CarService.class)
    @WithMethod("*")
    public void check(Car car){
        System.out.println("arg=" + car);
        System.out.println("check menu permission" + menuManager.getMenuList(current));
    }
}
