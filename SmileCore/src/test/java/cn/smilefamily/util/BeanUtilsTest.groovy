package cn.smilefamily.util

import cn.smilefamily.context.IllegalFileURLFormatException
import spock.lang.Specification

class BeanUtilsTest extends Specification {
    def "GetActiveProfilePath"() {
        expect:
        BeanUtils.getActiveProfilePath("classpath:application.properties", "dev") == "classpath:application-dev.properties"
        when:
        BeanUtils.getActiveProfilePath("classpath:application", "dev")
        then:
        def e = thrown(IllegalFileURLFormatException)
    }
}
