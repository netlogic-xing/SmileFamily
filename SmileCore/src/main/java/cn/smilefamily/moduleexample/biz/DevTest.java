package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.moduleexample.common.CommonConfig;
import cn.smilefamily.moduleexample.permission.PermissionConfig;
import cn.smilefamily.moduleexample.portal.PortalConfig;
import cn.smilefamily.util.SmileUtils;

public class DevTest {
    public static void main(String[] args) {
        SmileUtils.inspectConfig(CommonConfig.class);
        SmileUtils.inspectConfig(PortalConfig.class);
        SmileUtils.inspectConfig(PermissionConfig.class);
        SmileUtils.inspectConfig(AppConfig.class);
    }
}
