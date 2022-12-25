package cn.smilefamily.context.test;

import cn.smilefamily.annotation.core.Alias;
import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Factory;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.Value;

@Bean
@Alias("beanb")
public class BeanB {
    @Injected
    private BeanA beanA;
    @Injected(name = "/family")
    private Family family;

    public Family getFamily() {
        return family;
    }

    private String description;

    @Factory
    public BeanB(@Value("${BeanB.description:this is bean b}") String description) {
        this.description = description;
    }


    public BeanA getBeanA() {
        return beanA;
    }

    public void setBeanA(BeanA beanA) {
        this.beanA = beanA;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
