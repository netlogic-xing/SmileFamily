package cn.smilefamily.web.base

import cn.smilefamily.context.BeanContext
import cn.smilefamily.web.base.test.Config
import cn.smilefamily.web.base.test.HelloController
import org.reflections.Reflections
import spock.lang.Specification

class WebFrameworkTest extends Specification {
    def "controller"(){
        given:
        BeanContext beanContext = new BeanContext(Config.class);
        expect:
        beanContext.getBean(HelloController.class) != null
    }
}
