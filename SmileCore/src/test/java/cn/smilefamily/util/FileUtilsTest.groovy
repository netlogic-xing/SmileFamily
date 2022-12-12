package cn.smilefamily.util

import spock.lang.Specification

class FileUtilsTest extends Specification {
    def "GetInputStream"() {
        expect:
        FileUtils.getInputStream("classpath:application.yml") != null;
        FileUtils.getInputStream("classpath:test1.yml") != null;
        when:
        FileUtils.getInputStream("classpath:cn/smilefamily/context/test1.yml");
        then:
        thrown(RuntimeException)
    }

    def "PropertiesFrom"() {
    }
}
