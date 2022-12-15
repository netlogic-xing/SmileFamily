package cn.smilefamily.context

import cn.smilefamily.annotation.core.Profile
import cn.smilefamily.context.test.*
import cn.smilefamily.iocexample.external.Driver
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import spock.lang.Specification

class BeanContextTest extends Specification {
    def "GetName"() {

    }

    def "SetParent"() {
    }

    def "Export"() {
    }

    def "ImportBeanDefinitions"() {
    }

    def "GetBeanDefinitions"() {
    }

    def "GetBean[only initProperties]"() {
        def dummyProperties = [nameplate: "hostauto"]
        BeanContextHelper helper = Spy(new BeanContextHelper(), {
            propertiesFrom(_) >> Optional.of(dummyProperties)
        })
        BeanContext.setHelper(helper)
        BeanContext context = new BeanContext("classpath:dummy.properties");

        when:
        context.build()
        then:
        context.getBean("nameplate") == "hostauto"
        context.getBean("not-exist") == null
    }

    def "GetBean[only init yml]"() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        String yml = """
family:
    dad: 
        name: superman
        age: 42
    mum: 
        name: wonderwoman
        age: 41
    children:
        - name: littleprincess
          age: 6
        - name: iceprincess
          age: 15
"""
        BeanContextHelper helper = Spy(new BeanContextHelper(), {
            buildParser(_) >> {
                Optional.of(mapper.createParser(yml))
            }
        })
        BeanContext.setHelper(helper)
        BeanContext context = new BeanContext("classpath:dummy.yml");

        when:
        context.build()
        then:
        context.getBean("/family/dad/name") == "superman"
        context.getBean("family.dad.name") == "superman"
        context.getBean("family.children[0].name") == "littleprincess"
        context.getBean("not-exist") == null
    }

    def "GetBean[only init yml with placeholder]"() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        String yml = '''
family:
    ${he}: 
        name: ${he.name}
        age: 42
    ${her}: 
        name: wonderwoman
        age: 41
    children:
        - name: littleprincess
          age: 6
        - name: iceprincess
          age: 15
he: dad
her: mum
he.name: superman
'''
        BeanContextHelper helper = Spy(new BeanContextHelper(), {
            buildParser(_) >> {
                Optional.of(mapper.createParser(yml))
            }
        })
        BeanContext.setHelper(helper)
        BeanContext context = new BeanContext("classpath:dummy.yml");

        when:
        context.build()
        then:
        context.getBean("/family/dad/name") == "superman"
        context.getBean('family.${he}.name') == '${he.name}'
        context.getBean("family.dad.name") == "superman"
        context.getBean("family.children[0].name") == "littleprincess"
        context.getBean("not-exist") == null
    }

    def "GetBean[normal]"() {
        System.setProperty(Profile.ACTIVE_KEY, "dev")
        BeanContext context = new BeanContext(AppTestConfig.class)
        expect:
        context.getProfile() == "dev"
        when:
        context.build()
        then:
        context.getBean("/family/dad/name") == "ironman"
        context.getBean('family.${he}.name') == '${he.name}'
        context.getBean("family.dad.name") == "ironman"
        context.getBean("family.children[0].name") == "littleprincess"
        context.getBean('he') == "dad"
        context.getBean("not-exist") == null
        context.getBean(Driver.class) == null
        context.getBean(BeanA.class) != null
        BeanB b = context.getBean(BeanB.class)
        BeanB aliasB = context.getBean("beanb", BeanB.class)
        b == aliasB
        b.family.children[0].name == 'littleprincess'
        context.getBean(BeanD.class) == null
        Person thePerson = context.getBean("thePerson", Person.class)
        Person superman = context.getBean("superman", Person.class)
        Person ironman = context.getBean("ironman", Person.class)
        thePerson != null
        superman != null
        ironman != null
        thePerson == superman
        superman == ironman
    }

    def "TestGetBean"() {
    }

    def "CreateScope"() {
    }

    def "DestroyScope"() {
    }

    def "GetBeansByAnnotation"() {
    }

    def "Build"() {
    }

    def "PutBean"() {
    }
}
