package cn.smilefamily.util

import spock.lang.Specification

class FileUtilsTest extends Specification {
    def "GetInputStream"() {
        expect:
        FileUtils.getInputStream("classpath:application.yml").isPresent()
        FileUtils.getInputStream("classpath:test1.yml").isPresent()
        FileUtils.getInputStream("classpath:cn/smilefamily/context/test1.yml").isEmpty()
    }

    def "PropertiesFrom"() {
    }
}
