package cn.smilefamily.common;

import com.google.common.collect.ImmutableMap;
import org.snakeyaml.engine.v2.api.*;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.Tag;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlTest {
    public static void main(String[] args) throws FileNotFoundException {
        Map<Tag, ConstructNode> tags = new HashMap<>();
        tags.put(new Tag("!MyString"), new ConstructNode() {
            @Override
            public Object construct(Node node) {
                return node.toString() ;
            }
        });
        LoadSettings settings = LoadSettings.builder()
                .setLabel("Custom user configuration")
                .setAllowRecursiveKeys(true)
                .setTagConstructors(tags)
                .build();

        Load load = new Load(settings);
        Iterable<Object> confs = load.loadAllFromReader(new BufferedReader(new FileReader("common/src/main/resources/test.yml")));
        confs.forEach(c->{
            System.out.println(c);
        });
    }

    private static void test1() {
        LoadSettings settings = LoadSettings.builder().setLabel("Custom user configuration").build();
        Load load = new Load(settings);
        String document = "\n- Hesperiidae\n- Papilionidae\n- Apatelodidae\n- Epiplemidae";
        List<String> list = (List<String>) load.loadFromString(document);
        System.out.println(list);
        DumpSettings dsettings = DumpSettings.builder().build();
        Dump dump = new Dump(dsettings);
        String output = dump.dumpToString(ImmutableMap.of("x", 1, "y", 2, "z", 3));
        System.out.println(output);
    }
}
