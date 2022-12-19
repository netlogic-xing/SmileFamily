
# 简介
Spring全家桶广泛应用在企业应用开发中，为广大程序员解决了不少问题。但发展至今，spring已经变得越来越臃肿。
其庞大的规模让开发者很难此吃透，其极高的复杂多导致一旦出现问题调查很困难。
***********
SmileFamily旨在甩掉Spring的历史包袱，利用近年新的技术打造一套灵活、简单、透明的开发框架。
## 灵活性
SpringIoC框架属于静态注入框架。使用Spring，所有的Bean定义无论是通过Xml还在是注解，都必须在应用启动前定义好。无法根据业务需求动态添加或删除。
Spring Bean具有传染性，一个对象如果依赖某一个Spring托管的Bean，那么自身也必须被托管。这种传染性的后果是几乎所有的对象都被spring托管，
导致设计模式固化，不能根据业务需求采取最合适的设计方案。
****************
SmileIoC框架支持动态注入，可根据业务需求动态创建或注销Bean。SmileIoC不会限制设计者手脚，而是协助设计者。
## 简单性
Spring或者说整个java世界都存在过度设计的问题。仿佛一个东西不引入几个模式，不足以体现设计能力，仿佛没有几个接口、
工厂就不会使用java。这实际上已经违背了Rod Johnson不预想开发的思想。
****************
Smile强调设计的直接性，如果没有充足的理由就不引入过度的模式。
## 透明性
Spring等多数java框架设计复杂，代码庞大，在设计时并未把让使用者阅读、理解当作一个设计目标。
这导致了一旦使用中出现问题很难通过源码查找。整个框架对使用者相当于一个黑盒。
****************
Smile把让使用者理解当作重要设计目标，Smile鼓励使用者在使用中发现问题后阅读和debug框架源码。Smile相当与一个白盒。
# 架构[设计文档](/doc/design.md)
Smile家族包括:
1. SmileCore----SmileIoC容器
2. SmileWebFramework----Web MVC开发框架
3. SmileBoot----all-in-one的开发框架
4. SmileTemplate----简单server端模版语言
# Todo List

- [ ] 事件的支持
- [x] 模块化支持
    - [x] Bean的导入导出
    - [x] 模块的封装形式
- [x] @AliasFor的支持
- [x] Annotation"继承"的支持
- [x] Bean别名的支持
- [ ] Schedule的支持
- [x] AOP的支持
- [x] yml/json配置支持
- [x] profile的支持 
- [ ] 重新抽象接口，稳定接口
- [ ] 文档
    - [ ] 推广PPT
    - [ ] 设计文档
    - [ ] Getting started
    - [ ] User manuals
- [ ] SmileTemplate完善
- [ ] 对JDBC/JPA等的封装
- [ ] vert.x/异步方式支持

--------------------
>此分支采用aspectj实现