# 插件代码开发手册

本文档主要针对**Sermant**的[示例模块](../../sermant-plugins/sermant-example)，介绍插件开发过程中常见的一些场景。

- [组成部分](#组成部分)
- [插件模块](#插件模块)
  - [增强定义](#增强定义)
    - [原生类增强](#原生类增强)
  - [拦截器](#拦截器)
  - [插件配置](#插件配置)
  - [插件服务](#插件服务)
    - [简单插件服务](#简单插件服务)
    - [复杂插件服务](#复杂插件服务)
    - [日志功能](#日志功能)
    - [心跳功能](#心跳功能)
    - [链路功能](#链路功能)
    - [动态配置功能](#动态配置功能)
- [插件服务模块](#插件服务模块)

## 组成部分

由[插件模块开发手册](dev_plugin_module.md)可知，一个`插件主模块(main)`中可能包含以下5种内容：

- `插件模块(plugin)`，该模块主要用于声明对宿主应用的增强逻辑
- `服务模块(service)`，用于为插件包提供服务实现
- `后端模块(server)`，用于接收插件数据的服务端
- `前端模块(webapp)`，用于对服务端数据作前端展示
- `其他模块(other)`，特殊附加件，一般用作调试

考虑到后三者随实际业务场景不同有较大变化，因此赋予他们的开发自由度较高，对他们仅有模块目录和输出目录的限制。出于这点考虑，`示例模块`将不对他们做参考案例。`示例模块`中包含以下模块：

- [demo-plugin](../../sermant-example/demo-plugin): 示例插件
- [demo-service](../../sermant-plugins/sermant-example/demo-service): 示例插件服务
- [demo-application](../../sermant-plugins/sermant-example/demo-application): 示例宿主应用

## 插件模块

示例插件模块`demo-plugin`中，主要用于向插件开发者展示在插件开发过程中可能遇到的一些场景以及可能使用到的一些功能。

开始之前，必须约定的就是，`插件模块(plugin)`中，开发者只能使用*Java*原生API和[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)中的API，不能依赖或使用任何`byte-buddy`与`slf4j`以外的第三方依赖。如果应业务要求，需要使用其他第三方依赖的话，只能在`插件模块(plugin)`中定义接口，在`服务模块(service)`中编写实现。更多相关内容详见[插件模块开发手册](dev_plugin_module.md#添加插件模块)。

### 增强定义

**Sermant**的核心能力是对宿主应用做非侵入式的字节码增强，而这些增强规则则是插件化的。在每个**Sermant**的`插件主模块(main)`中，都可以定义一些增强定义，针对宿主应用的某些特定方法进行字节码增强，从而实现某种功能。因此`插件主模块(main)`如何告知**Sermant**该增强哪些类，是一个重要的课题。

插件的增强定义需要实现[PluginDeclarer](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/declarer/PluginDeclarer.java)接口，其中包含两个接口方法：

- `getClassMatcher`方法用于获取被增强类的匹配器[ClassMatcher](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/matcher/ClassMatcher.java)。
- `getInterceptDeclarers`方法用于获取被增强类的拦截点方法，以及嵌入其中的拦截器，他们封装于方法拦截点[InterceptDeclarer](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/declarer/InterceptDeclarer.java)中。
- `getSuperTpeDecarers`方法用于获取插件的超类声明[SuperTypeDeclarer](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/declarer/SuperTypeDeclarer.java)

对匹配器 [ClassMatcher](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/matcher/ClassMatcher.java)，在核心模块中提供了两种类型的匹配器：

[ClassTypeMatcher](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/matcher/ClassTypeMatcher.java)(类的类型匹配器)

- 完全通过名称匹配，也是最常见的定位方式，通过以下方法获取：
  ```java
  ClassMatcher.nameEquals("${class reference}");
  ```
  其中`${class reference}`为被增强类的全限定名。


- 通过名称匹配多个类，属于`nameEquals`的复数版，可通过以下方法获取：
  ```java
  ClassMatcher.nameContains("${class reference array}");
  ```
  其中`${class reference array}`为被增强类的全限定名可变数组。

[ClassFuzzyMatcher](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/matcher/ClassFuzzyMatcher.java)（类的模糊匹配器）

- 通过全限定名前缀定位到被增强类，可通过以下方法获取：
  ```java
  ClassMatcher.namePrefixedWith("${prefix}");
  ```
  其中`${prefix}`为全限定名前缀。


- 通过全限定名后缀定位到被增强类，可以通过一下方法获取：
  ```java
  ClassMatcher.nameSuffixedWith("${suffix}")
  ```
  其中`${suffix}`为全限定名后缀。


- 通过全限定名内缀定位到被增强类，可以通过以下方法获取：
  ```java
  ClassMatcher.nameinfixedWith("${infix}")
  ```
  其中`${infix}`为全限定名内缀。


- 通过正则表达式匹配全限定名定位到被增强类，可以通过以下方法获取：
  ```java
  ClassMatcher.nameMatches("${pattern}")
  ```
  其中`${pattern}`为正则表达式。


- 通过注解定位到被该注解修饰的类，可通过以下方法获取：
  ```java
  ClassMatcher.isAnnotationWith("${annotation reference array}");
  ```
  其中`${annotation reference array}`为注解的全限定名可变数组。


- 通过超类定位到该类的子类，可通过以下方法获取：
  ```java
  ClassMatcher.isExtendedFrom("${super class array}");
  ```
  其中`${super class array}`为超类可变数组。考虑到Java的继承规则，该数组只能有一个`Class`，其余必须全为`Interface`。


- 匹配的逻辑操作，匹配器全部不匹配时为真：
  ```java
  ClassMatcher.not("${class matcher array}")
  ```
  其中`${class matcher array}`为匹配器可变长数组


- 匹配的逻辑操作，匹配器全部都匹配时为真：
  ```java
  ClassMatcher.and("${class matcher array}")
  ```
  其中`${class matcher array}`为匹配器可变长数组


- 匹配的逻辑操作，匹配器其中一个匹配时为真：
  ```java
  ClassMatcher.or("${class matcher array}")
  ```
  其中`${class matcher array}`为匹配器可变长数组

对于方法拦截点[MethodMatcher](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/matcher/MethodMatcher.java)，提供了多种匹配方法：

- 全数匹配：
  ```java
  MethodMatcher.any();
  ```
- 名称匹配：
  ```java
  MethodMatcher.nameEquals("${method name}");
  ```
  其中`${method name}`为方法名称。


- 匹配静态方法：
  ```java
  MethodMatcher.isStaticMethod();
  ```
- 匹配构造函数：
  ```java
  MethodMatcher.isConstructor();
  ```
- 匹配多个方法：
  ```java
  MethodMatcher.nameContains("${method name array}");
  ```
  其中`${method name array}`为方法名称数组。


- 根据方法名称前缀匹配：
  ```java
  MethodMatcher.namePrefixedWith("${method name prefix}");
  ```
  其中`${method name prefix}`为方法名称前缀。


- 根据方法名称后缀匹配：
  ```java
  MethodMatcher.nameSuffixedWith("${method name suffix}");
  ```
  其中`${method name suffix}`为方法名称后缀。


- 根据方法名称内缀匹配：
  ```java
  MethodMatcher.nameinfixedWith("${method name infix}");
  ```
  其中`${method name infix}`为方法名称内缀。


- 根据正则表达式匹配：
  ```java
  MethodMatcher.nameMatches("${pattern}");
  ```
  其中`${pattern}`为正则表达式。


- 匹配被传入注解修饰的方法：
  ```java
  MethodMatcher.isAnnotatedWith("${annotations array}");
  ```
  其中`${annotations array}`为注解集。


- 匹配指定入参数量的方法：
  ```java
  MethodMatcher.paramCountEquals("${param count}");
  ```
  其中`${param count}`为入参数量。


- 匹配指定入参类型的方法：
  ```java
  MethodMatcher.paramTypeEquals("${param type array}");
  ```
  其中`${param type array}`为入参类型集。


- 匹配指定返回值类型的方法：
  ```java
  MethodMatcher.resultTypeEquals("${result type}");
  ```
  其中`${result type}`返回值类型。


- 逻辑操作，方法匹配器集全不匹配时则结果为真
  ```java
  MethodMatcher.not("${element matcher array}");
  ```
  其中`${element matcher array}`为方法匹配器集。


- 逻辑操作，方法匹配器集全匹配时则结果为真
  ```java
  MethodMatcher.and("${element matcher array}");
  ```
  其中`${element matcher array}`为方法匹配器集。


- 逻辑操作，方法匹配器集其中一个匹配时则结果为真
  ```java
  MethodMatcher.or("${element matcher array}");
  ```
  其中`${element matcher array}`为方法匹配器集。

更多方法匹配方式可以参考[byte-buddy](https://javadoc.io/doc/net.bytebuddy/byte-buddy/latest/net/bytebuddy/matcher/ElementMatchers.html)中含`MethodDescription`泛型的方法。

开发到最后，不要忘记添加`PluginDeclarer`接口的*SPI*配置文件：

- 在资源目录`resources`下添加`META-INF/services`文件夹。
- 在`META-INF/services`中添加`com.huaweicloud.sermant.core.plugin.agent.declarer.PluginDeclarer`配置文件。
- 在上述文件中，以换行为分隔，键入插件包中所有的增强定义`PluginDeclarer`实现。

**Sermant**的`示例模块`中包含以下`PluginDeclarer`接口的实现示例：

- [DemoAnnotationDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoAnnotationDeclarer.java): 通过注解定位被修饰类的普通增强定义
- [DemoNameDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoNameDeclarer.java): 通过名称定位到被增强类的普通增强定义
- [DemoSuperTypeDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoSuperTypeDeclarer.java): 通过超类定位到被增强子类的普通增强定义
- [DemoBootstrapDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoBootstrapDeclarer.java): 对启动类加载器进行增强的定义，详见[原生类增强](#原生类增强)一节
- [DemoTraceDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoTraceDeclarer.java): 对示例应用使用链路功能的增强定义，详见[链路功能](#链路功能)一节

在各插件开发者在编写插件增强定义的时候，可以以以上示例为参考，开发符合自身需要的增强定义。

#### 原生类增强

对于`java.lang.Thread`等*Java*原生类，他们由启动类加载器`BootStrapClassLoader`加载，对他们进行增强的话，主要会面临两个困难：

- 原生类被启动类加载器加载，那么如果对他们做增强的话，就需要将被增强后的字节码重新覆盖回启动类加载器中。考虑到增强后的嵌入代码主要是在**拦截器**中编写的，而这些内容主要由系统类加载器`AppClassLoader`加载，启动类加载器无法访问。得益于[**Advice模板类**](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/agent/template)中使用反射调用拦截器的方法，使得原生类在增强时，编写的拦截器不受拘束。

- 鉴于*Java*重定义*Class*的限制，我们无法修改这些原生类的元信息，那么就无法使用[**byte-buddy委派**](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/agent/transformer/DelegateTransformer.java)的方式对他们进行增强(原理是添加委派属性和静态代码块)。所幸我们可以使用[**Advice模板类**](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/agent/template)配合[**byte-buddy advice**](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/agent/transformer/BootstrapTransformer.java)技术进行增强，而现在**核心模块**就是这么做的。两种风格统合之后，前者用于处理系统类加载器加载的普通类，后者用于处理启动类加载器的原生类。

结合上述内容，其实增强原生类和增强普通类在增强定义和拦截器编写上没有什么区别，但是还是希望插件开发者尽量少地对原生类进行增强，原因有三：

- 对原生类的增强往往是发散的，对他们增强很可能会对其他插件或宿主功能造成影响。
- 对原生类的增强逻辑，将使用反射的方式调用系统类加载器中的拦截器方法。由于*Java*重定义*Class*的限制，每次调用被增强方法的时候，都会进行反射处理的逻辑，这将极大限制该方法的*TPS*上限。
- 对原生类的增强过程中，涉及到使用**Advice模板类**生成动态拦截类。对于每个被增强的原生类方法，都会动态生成一个，他们将被系统类加载器加载。如果不加限制的增强原生类，加载动态类也会成为启动过程中不小的负担。

综上，[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)中提供对*Java*原生类增强的能力，但是，不建议不加限制地对他们进行增强，如果有多个增强点可选，优先考虑增强普通类。

**Sermant**的`示例模块`中，[DemoBootstrapDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoBootstrapDeclarer.java)对`java.lang.Thread`做了增强，可以启动示例应用[DemoApplication](../../sermant-plugins/sermant-example/demo-application/src/main/java/com/huawei/example/demo/DemoApplication.java)观察`java.lang.Thread`是否被正常增强。

### 拦截器

新版插件的开发中在拦截器层面不再对静态方法、构造函数和实例方法作区分，降低插件开发的复杂度
对于方法拦截点`MethodInterceptPoint`，有静态方法、构造函数和实例方法三种获取类型，对应的拦截器也有三种类型：

- [InterCeptor](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/agent/interceptor/Interceptor.java): 拦截器接口，其中包含三个方法：
  - `before`，前置方法，该方法在拦截点之前执行。ExecuteContext参数为插件执行的上下文，里面封装拦截器运作所需的所有参数，通过skip方法可跳过主要流程，并设置最终方法结果，注意，增强构造函数时，不能跳过主要流程
  - `after`，后置方法，无论被拦截方法是否正常执行，最后都会进入后置方法中。后置方法可以通过返回值覆盖被拦截方法的返回值，因此这里开发者需要注意不要轻易返回null。
  - `onThrow`，处理异常的方法，当被拦截方法执行异常时触发。这里处理异常并不会影响异常的正常抛出。

**Sermant**的`示例模块`中包含以下拦截器示例：

- [DemoStaticInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoStaticInterceptor.java): 普通的静态方法拦截器
- [DemoConstInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoConstInterceptor.java): 普通的构造函数拦截器
- [DemoMemberInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoMemberInterceptor.java): 普通的实例方法拦截器
- [DemoConfigInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoConfigInterceptor.java): 插件配置使用示例拦截器，详见[插件配置](#插件配置)一节
- [DemoServiceInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoServiceInterceptor.java): 插件服务使用示例拦截器，详见[插件服务](#插件服务)一节

在各插件开发者在编写自定义拦截器的时候，可以以以上示例为参考，开发满足自身功能需要的拦截器。

### 插件配置

**插件配置**指的是在插件包和插件服务包使用的配置系统，主要由三部分组成：

- [PluginConfig](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/config/PluginConfig.java)插件配置接口: 所有普通的插件配置都要实现该接口。
- [AliaConfig](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/config/AliaConfig.java)别名插件配置抽象类: 在插件配置`PluginConfig`的基础上，如果需要设定拦截器别名，则改为继承该抽象类。
- [PluginConfigManager](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/config/PluginConfigManager.java)插件配置管理器: 提供获取插件配置`PluginConfig`的接口：
  ```java
  // ${plugin config class}为插件配置的Class
  PluginConfigManager.getPluginConfig(${plugin config class});
  ```
  `PluginConfigManager`是统一配置管理器[ConfigManager](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/config/ConfigManager.java)的特例，插件端可以直接使用后者的接口获取插件配置和统一配置：
  ```java
  // ${base config class}为统一配置或插件配置的Class
  ConfigManager.getConfig(${base config class});
  ```

从**Sermant**的`示例模块`的插件配置文件[config.yaml](../../sermant-plugins/sermant-example/config/config.yaml)可以看出，该配置文件是一个*yaml*文件，一个`插件主模块(main)`的`插件模块(plugin)`和`服务模块(service)`中的插件配置对应的配置信息都封装在这唯一的`config.yaml`中。

相较于传统的*yaml*格式配置文件对应一个*Java Pojo*对象，这里的`config.yaml`可以封装多个*Java Pojo*，他们由全限定名或别名进行区分，形成类似*Map*的结构，其中`demo.test`键对应的是**示例插件包**中的[DemoConfig](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/config/DemoConfig.java)对象，`com.huawei.example.demo.config.DemoServiceConfig`键对应的是**示例插件服务包**中的[DemoServiceConfig](../../sermant-plugins/sermant-example/demo-service/src/main/java/com/huawei/example/demo/config/DemoServiceConfig.java)对象。

[DemoConfig](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/config/DemoConfig.java)相较于`DemoServiceConfig`，前者被[ConfigTypeKey](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/config/common/ConfigTypeKey.java)注解修饰，因此设定了`demo.test`的别名。如果没有被`ConfigTypeKey`注解修饰，则直接使用全限定名做索引。

[DemoConfig](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/config/DemoConfig.java)继承了[AliaConfig](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/config/AliaConfig.java)，因此他自动继承了`pluginName`和`interceptors`两个属性，有对拦截器起别名的能力，这些配置在[config.yaml](../../sermant-plugins/sermant-example/config/config.yaml)中清晰可见，不做赘述。

[DemoConfig](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/config/DemoConfig.java)中的`map`属性被[ConfigFieldKey](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/config/common/ConfigFieldKey.java)注解修饰，将其属性名修改为`str2DemoSimplePojoMap`。不过需要注意的是，使用该注解的**Java Pojo**如果被数组、*List*或*Map*包装了一层，该注解的语义无效，因此，目前该注解只能用于修饰当前**插件配置类**或直接使用的**Java Pojo**。

从[DemoConfig](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/config/DemoConfig.java)中可以看出，插件配置现在支持的数据类型包括：

- 布尔、数值类的基础类型及包装类型
- 字符串类型
- 枚举类型
- 复杂对象类型
- 上述类型构成的数组
- 前四种类型构成的*List*
- 前四种类型构成的*Map*

*yaml*格式配置文件目前还有一些其他规则：

- 对于数组、List和Map中涉及的复杂对象，不支持`ConfigFieldKey`修正属性名
- 对于数组、List和Map中的字符串，不支持`${}`转换，**插件配置类**的字符串属性和复杂类型属性内部的字符串属性支持
- 仅在字符串做`${}`转换时使用入参，不支持使用入参直接设置属性值

[DemoConfig](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/config/DemoConfig.java)中已经包含了大部分可能的配置场景，插件开发者可以与之参考，编写符合自身业务需求的插件配置类。

最后，不要忘记添加插件配置的*SPI*配置文件：

- 在资源目录`resources`下添加`META-INF/services`文件夹。
- 在`META-INF/services`中添加`com.huaweicloud.sermant.core.plugin.config.PluginConfig`配置文件。
- 在上述文件中，以换行为分隔，键入插件包中所有的插件配置`PluginConfig`或`AliaConfig`实现。

### 插件服务

**插件服务**指的是在插件包和插件服务包使用的服务系统，主要由两部分组成：

- [PluginService](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/service/PluginService.java)插件服务接口。
- [PluginServiceManager](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/service/PluginServiceManager.java)插件服务管理器，其中提供获取`PluginService`的接口：
  ```java
  // ${plugin service class}为插件服务的Class
  PluginServiceManager.getPluginService(${plugin service class});
  ```
  `PluginServiceManager`其实只是[ServiceManager](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/service/ServiceManager.java)的一个特例，可以直接使用后者的接口获取核心服务和插件服务：
  ```java
  // ${base service class}为核心服务或插件服务的Class
  ServiceManager.getService(${base service class});
  ```

  从[插件模块开发手册](dev_plugin_module.md#添加插件模块)可知，插件有**简单插件**和**复杂插件**之分，这主要和他们所定义服务的复杂程度有关：

  - **简单插件**中定义的服务只能使用*Java*原生*API*、[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)中自研的*API*(`com.huawei`开头)，以及`byte-buddy`和`slf4j`的*API*。
  - **复杂插件**中的服务除了能使用上述*API*，还有权使用其他第三方依赖的*API*。这些服务需要分离出**插件服务接口**和**插件服务实现**：前者编写于`插件模块(plugin)`中，供拦截器调用；后者编写于`服务模块(service)`中，由自定义*ClassLoader*加载，以实现类加载器级别的依赖隔离。

#### 简单插件服务

对于**简单插件服务**，实现[PluginService](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/service/PluginService.java)接口即可，如[DemoSimpleService](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/service/DemoSimpleService.java)示例，在实现`start`方法和`stop`方法的基础上，可以添加其他所需的方法，如`activeFunc`方法，通过以下代码获得`DemoSimpleService`的示例并调用`activeFunc`方法：
```java
DemoSimpleService simpleService = PluginServiceManager.getPluginService(DemoSimpleService.class);
simpleService.activeFunc();
```

对于**简单插件服务**来说，唯一的限制就是只能使用*Java*原生*API*，[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)中自研的*API*(`com.huawei`开头)，以及`byte-buddy`和`slf4j`的*API*。

插件开发者可以参照[DemoSimpleService](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/service/DemoSimpleService.java)，按需编写自身业务所需的**简单插件服务**。

最后不要忘记添加[PluginService](../../sermant-agentcore/sermant-agentcore-core/src/main/java/com/huaweicloud/sermant/core/plugin/service/PluginService.java)的*SPI*配置文件：

- 在资源目录`resources`下添加`META-INF/services`文件夹。
- 在`META-INF/services`中添加`com.huaweicloud.sermant.core.plugin.service.PluginService`配置文件。
- 在上述文件中，以换行为分隔，键入插件包中所有的插件服务`PluginService`实现。

特别需要注意的是，不要尝试在`PluginService`的`start`方法中获取其他**插件服务**的实例，由于**插件服务**仍在初始化当中，可能无法正确获取这些**插件服务**实例。

#### 复杂插件服务

**复杂插件服务**比起**简单插件服务**，只有两点区别：

- **复杂插件服务**在`插件模块(plugin)`中编写接口，在[`服务模块(service)`](#插件服务模块)中编写实现，而**简单插件服务**不需要编写接口，直接在`插件模块(plugin)`中实现。
- **复杂插件服务**的实现可以按需使用任意第三方依赖。

[DemoComplexService](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/service/DemoComplexService.java)是**复杂插件服务**示例接口，其中可以按需添加接口，如`activeFunc`方法，[DemoComplexServiceImpl](../../sermant-plugins/sermant-example/demo-service/src/main/java/com/huawei/example/demo/service/DemoComplexServiceImpl.java)是对应的实现。我们可以通过以下代码调用`activeFunc`方法：
```java
DemoComplexService complexService = PluginServiceManager.getPluginService(DemoComplexService.class);
complexService.activeFunc();
```

添加*SPI*配置及其他注意事项和**简单插件服务**没有区别，这里不做赘述。开发者可以参照`DemoComplexService`接口和`DemoComplexServiceImpl`实现编写符合自身业务要求的**复杂插件服务**。

#### 日志功能

考虑到依赖隔离的问题，[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)提供给`插件模块(plugin)`和`服务模块(service)`使用的日志只能是**jul**日志，通过以下方法获取**jul**日志实例：

```java

import com.huaweicloud.sermant.core.common.LoggerFactory;
Logger logger=LoggerFactory.getLogger();
```

插件开发者如果需要输出日志信息，可以参考[DemoLogger](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/common/DemoLogger.java)示例开发。

#### 心跳功能

心跳功能是[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)的核心服务之一，通过以下代码获取实例：
```java
HeartbeatService heartbeatService = ServiceManager.getService(HeartbeatService.class);
```

心跳功能在初始化的时候就会启动执行，定期将每个插件的名称、版本等信息发送至后端服务器。目前来说，插件的心跳上报的信息包括：

- `hostname`：发送客户端的主机名
- `ip`：发送客户端的IP地址
- `app`：应用名称，即启动参数中的`appName`
- `appType`：应用类型，即启动参数中的`appType`
- `heartbeatVersion`：上一次心跳发送时间
- `lastHeartbeat`：上一次心跳发送时间
- `version`：核心包的版本，即核心包`manifest`文件的`Sermant-Version`值
- `pluginName`：插件名称，通过插件设定文件确定
- `pluginVersion`：插件版本号，取插件jar包中`manifest`文件的`Sermant-Plugin-Version`值

如果希望在插件上报的数据中增加额外的内容，可以调用以下api：
```java
// 通过自定义ExtInfoProvider提供额外内容集合
heartbeatService.setExtInfo(new ExtInfoProvider() {
  @Override
  public Map<String, String> getExtInfo() {
    // do something
  }
});
```

插件开发者如果需要往心跳功能发送的数据包中增加额外内容，可以参考[DemoHeartBeatService](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/service/DemoHeartBeatService.java)示例开发。

更多心跳服务相关内容可参见[心跳服务介绍](service_heartbeat.md)。

#### 链路功能

**链路功能**是一个继消息发送能力建立的一个上层功能，该功能简单来说就是从宿主端的调用链之间嵌入以下逻辑：

- 在发送数据的时候，在数据包中插入链路所需的`TraceId`和`SpanId`，前者是请求在分布式系统中的整个链路视图，后者代表整个链路中不同服务内部的视图。
- 在接收数据的时候，解析数据包中嵌入的链路相关内容，形成链路的一环提交到后台服务器中，逐渐形成调用链。

在示例宿主的[DemoTraceService](../../sermant-plugins/sermant-example/demo-application/src/main/java/com/huawei/example/demo/service/DemoTraceService.java)中，`counsumer`方法和`provider`方法模仿服务器接收数据并处理发送数据的流程，而数据包则假定存在一个`ThreadLocal`中，直到下一次调用`provider`方法接收数据。

基于上述示例宿主，我们编写[DemoTraceDeclarer](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/declarer/DemoTraceDeclarer.java)增强定义
，对`DemoTraceService`的`provider`方法和`consumer`方法分别使用使用[DemoTraceProviderInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoTraceProviderInterceptor.java)和[DemoTraceConsumerInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoTraceConsumerInterceptor.java)拦截器进行增强。

- 对于发送`provider`方法，做如下增强：
  ```java
    private final TracingService tracingService = ServiceManager.getService(TracingService.class);

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        TracingRequest request =
            new TracingRequest(context.getRawCls().getName(), context.getMethod().getName());
        ExtractService<HashMap<String, String>> extractService = (tracingRequest, carrier) -> {
            tracingRequest.setTraceId(carrier.get(TracingHeader.TRACE_ID.getValue()));
            tracingRequest.setParentSpanId(carrier.get(TracingHeader.PARENT_SPAN_ID.getValue()));
            tracingRequest.setSpanIdPrefix(carrier.get(TracingHeader.SPAN_ID_PREFIX.getValue()));
        };
        tracingService.onProviderSpanStart(request, extractService, (HashMap<String, String>)context.getArguments()[0]);
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        tracingService.onSpanFinally();
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        tracingService.onSpanError(context.getThrowable());
        return context;
    }
  ```
- 对于`consumer`方法，做如下增强：
  ```java
    TracingService tracingService = ServiceManager.getService(TracingService.class);

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        TracingRequest request =
            new TracingRequest(context.getRawCls().getName(), context.getMethod().getName());
        InjectService<HashMap<String, String>> injectService = (spanEvent, carrier) -> {
            carrier.put(TracingHeader.TRACE_ID.getValue(), spanEvent.getTraceId());
            carrier.put(TracingHeader.PARENT_SPAN_ID.getValue(), spanEvent.getSpanId());
            carrier.put(TracingHeader.SPAN_ID_PREFIX.getValue(), spanEvent.getNextSpanIdPrefix());
        };
        tracingService.onConsumerSpanStart(request, injectService, (HashMap<String, String>)context.getResult());
        tracingService.onSpanFinally();
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        tracingService.onSpanError(context.getThrowable());
        return context;
    }
  ```
  **Sermant**的`示例模块`这里只是简单地抛砖引玉。
  如果插件开发者需要使用链路功能，参照[DemoTraceInterceptor](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/interceptor/DemoTraceInterceptor.java)自行开发。


### 动态配置功能

动态配置功能是[**Sermant**核心功能模块](../../sermant-agentcore/sermant-agentcore-core)的核心服务之一，通过以下代码获取实例：
```java
DynamicConfigService service = ServiceManager.getService(DynamicConfigService.class);
```

调用以下方法注册一个监听器：
```java
// ${group}为用户分组，${key}为监听的键，对zookeeper来说，监听的路径相当于: / + ${group} + ${key}
// 如果不传${group}，则会默认设置为统一配置中dynamicconfig.default_group对应的值
service.addConfigListener("${key}", "${group}", new DynamicConfigListener() {
  @Override
  public void process(ConfigChangedEvent event) {
    // do something
  }
});
```

注册监听器之后，当服务器对应节点发生创建、删除、修改、添加子节点等事件时，就会触发`process`函数。

插件开发者如果需要使用动态配置，可以参考[DemoDynaConfService](../../sermant-example/demo-plugin/src/main/java/com/huawei/example/demo/service/DemoDynaConfService.java)示例开发。

更多动态配置服务相关内容可参见[动态配置服务介绍](service_dynamicconfig.md)。


## 插件服务模块

**插件服务模块**较**插件模块**相比：

- 只能编写**插件配置**和**插件服务接口**的实现，不能编写**增强定义**、**拦截器**和**插件服务接口**
- 允许自由添加需要的第三方依赖，打包的时候，需要提供输出依赖的方式，可以用`shade`插件或`assembly`插件打带依赖*jar*包，也可以直接使用`dependency`插件输出依赖包。

注意：由于`byte-buddy`和`slf4j`包的限制，还是建议直接使用`shade`插件打包，相关的规则已在[插件根模块](../../sermant-plugins)中定义完毕，直接引入插件即可，详见[插件模块开发手册](dev_plugin_module.md#添加插件服务模块)。

**插件服务模块**中通常涉及[插件配置](#插件配置)和[插件服务](#插件服务)的编写，其中**插件服务**主要是指[复杂插件服务](#复杂插件服务)的实现。以上相关内容前文已有介绍，这里不做赘述。

[返回**Sermant**说明文档](../README.md)