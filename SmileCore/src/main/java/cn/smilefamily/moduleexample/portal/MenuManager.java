package cn.smilefamily.moduleexample.portal;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Export;
import cn.smilefamily.annotation.core.External;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.moduleexample.PermissionChecker;
import cn.smilefamily.moduleexample.User;

import java.util.ArrayList;
import java.util.List;

@Bean
@Export("菜单管理器，由于注册菜单")
public class MenuManager {
    @Injected
    @External("需要外部提供PermissionChecker")
    private PermissionChecker checker;
    private String name;
    List<Menu> menuRegistry = new ArrayList<>();
    public List<Menu> getMenuList(User user){
       return menuRegistry.stream().filter(m->checker.hasPermission(m, user)).toList();
    }
    public void registerMenu(Menu menu){
        menuRegistry.add(menu);
    }
}
