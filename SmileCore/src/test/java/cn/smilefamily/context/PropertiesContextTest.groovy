package cn.smilefamily.context

import cn.smilefamily.BeanInitializationException
import spock.lang.Shared
import spock.lang.Specification

class PropertiesContextTest extends Specification {
    void setup() {
    }

    void cleanup() {
    }

    def "GetBean by name[simple key/value]"() {
        given: "put simple key=value pairs to "
        Map<String, String> properties = [host   : "superman",
                                          hostess: "wonderwoman"]
        Context host = Mock()
        def propertiesContext = new PropertiesContext(host)
        propertiesContext.addProperties("test1", properties)
        when: "properties context initialized"
        propertiesContext.initialize()
        then: "can get properties via context"
        propertiesContext.getBean("host") == "superman"
        propertiesContext.getBean("hostess") == "wonderwoman"

    }

    @Shared
    Map<String, String> testProperties = new HashMap<>()

    def "GetBean by name[iterate]"() {
        given: "put simple key=value pairs to "
        testProperties.put(key, value)
        Context host = Mock() {
            getName() >> "host"
            getBean(_, _) >> (name, _) -> testProperties.get(name)
        }
        def propertiesContext = new PropertiesContext(host)
        propertiesContext.addProperties("test1", testProperties)
        when: "properties context initialized"
        propertiesContext.initialize()
        propertiesContext.build()
        then: "can get properties via context"
        propertiesContext.getBean(realKey) == realVal
        where:
        key           | value          | realKey            | realVal
        "host"        | "superman" | key                | value
        "hostess"     | "wonderwoman"    | key                | value
        '${host}_age' | "42"           | "superman_age" | "42"// key is placeholder
    }

    def "GetBean by name[placeholder]"() {
        given: "put simple key=value pairs to "
        Map<String, String> testProperties =
                ['${nameplate}_ip'           : "30.0.11.82",
                 nameplate                   : "hostauto",
                 "@appName"                   : "HostAuto",
                 '@nameplate'                : '${nameplate}',
                 '${nameplate}_port'         : "30",
                 '${nameplate}_path'         : '/hostauto',
                 '${nameplate}_absolute_path': '${user.dir}',
                 '${nameplate}_url'          : 'http://${${nameplate}_ip}:${${nameplate}_port}${${nameplate}_path}/query?task=$\\{id\\}']

        Context host = Mock() {
            getName() >> "host"
        }
        def propertiesContext = new PropertiesContext(host)
        interaction {
            host.getBean(_, _) >> (name, _) -> {
                propertiesContext.getBean(name)
            }
        }
        propertiesContext.addProperties("test1", testProperties)
        when: "properties context initialized"
        propertiesContext.initialize()
        propertiesContext.build()
        then: "can get properties via context"
        propertiesContext.getBean("hostauto_ip") == "30.0.11.82"
        propertiesContext.getBean("hostauto_absolute_path") == System.getProperty("user.dir")
        propertiesContext.getBean("hostauto_url") == 'http://30.0.11.82:30/hostauto/query?task=${id}'
        propertiesContext.export().size() == 2
        propertiesContext.getBean("@nameplate") == "hostauto"
        propertiesContext.getBean("@appName") == "HostAuto"
        propertiesContext.getBean("hostauto_port", String.class) == "30"
    }

    def "GetBean by name[placeholder not found]"() {
        given: "put simple key=value pairs to "
        Map<String, String> testProperties =
                ['${nameplate}_ip'           : "30.0.11.82",
                 '${nameplate}_url'          : 'http://${${nameplate}_ip}:${${nameplate}_port}${${nameplate}_path}/query?task=$\\{id\\}']

        Context host = Mock() {
            getName() >> "host"
        }
        def propertiesContext = new PropertiesContext(host)
        interaction {
            host.getBean(_, _) >> (name, _) -> {
                propertiesContext.getBean(name)
            }
        }
        propertiesContext.addProperties("test1", testProperties)
        when: "properties context initialized"
        propertiesContext.initialize()
        propertiesContext.build()
        then: "can get properties via context"
        thrown(BeanInitializationException)
    }

    def "build[repeat build]"() {
        given: "put simple key=value pairs to "
        Map<String, String> testProperties =[:]

        Context host = Mock() {
            getName() >> "host"
        }
        def propertiesContext = new PropertiesContext(host)
        interaction {
            host.getBean(_, _) >> (name, _) -> {
                propertiesContext.getBean(name)
            }
        }
        propertiesContext.addProperties("test1", testProperties)
        when: "properties context initialized"
        propertiesContext.initialize()
        propertiesContext.build()
        propertiesContext.build()
        then: "can get properties via context"
        noExceptionThrown()
    }
    def "PropertiesContext's name is host's name"() {
        given:
        Context host = Mock() {
            getName() >> "root"
        }
        when:
        PropertiesContext propertiesContext = new PropertiesContext(host)
        then:
        propertiesContext.getName() == host.getName();
    }


}
