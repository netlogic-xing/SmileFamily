package cn.smilefamily.context

import cn.smilefamily.context.test.Family
import cn.smilefamily.context.test.Person
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import spock.lang.Specification

class YamlContextTest extends Specification {
    def "GetBean"() {
        YamlContext context = new YamlContext()
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        def type = mapper.typeFactory.constructParametricType(List.class, Person.class);
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
        when:
        context.addYamlDoc("test", mapper.readTree(yml))
        then:
        Family bean = context.getBean("/family", Family.class)
        bean.getDad().getName() == "superman"
        bean.getMum().getAge() == 41
        bean.getChildren().size() == 2
        List<Person> children = context.getBean("/family/children", mapper.constructType(type))
        children.size() == 2
        children[0].name == "littleprincess"
        String name = context.getBean("/family/dad/name")
        name == "superman"
        int age = context.getBean("/family/children/1/age")
        age == 15
        context.getBean("/family/children/2/name") == null
        context.getBean("family") == null
    }


    def "GetName"() {
        given:
        Context host = Mock() {
            getName() >> "host"
        }
        YamlContext yamlContext = new YamlContext(host)
        expect:
        yamlContext.getName() == "host"
    }



    def "Build"() {
        YamlContext context = new YamlContext()
        when:
        context.build()
        then:
        noExceptionThrown()
    }
}
