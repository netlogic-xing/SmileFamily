package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.annotation.aop.ScanAspect;
import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.ScanPackage;

@Configuration
@ScanPackage("cn.smilefamily.moduleexample.biz")
@ScanAspect("cn.smilefamily.moduleexample.biz")
public class AppConfig {
}
