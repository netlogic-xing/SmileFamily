package cn.smilefamily.moduleexample.permission;

import cn.smilefamily.annotation.core.External;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.moduleexample.PermissionChecker;
import cn.smilefamily.moduleexample.PermissionSubject;
import cn.smilefamily.moduleexample.User;
import cn.smilefamily.moduleexample.common.Datasource;

public class DatabasePermissionChecker implements PermissionChecker {
    @Injected
    @External("使用公共数据源")
    private Datasource datasource;
    @Override
    public boolean hasPermission(PermissionSubject subject, User user) {
        System.out.println("check permission for " + user);
        return true;
    }
}
