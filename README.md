## eventbus-spring-boot-starter

事件分发组件

### 概述
在我们日常开发中通常为了和某个业务进行解耦合操作，通常会等待某个业务处理完成之后后面的业务会交给其它业务
服务进行处理。常见的例子就是在我们在线购物平台进行购物下单成功之后，一会儿之后我们会收到一条短信或者是邮件
提示信息，而这个通知消息通常我们为了和订单业务解耦就可以发布一条消息交给通知业务服务进行处理了，而在我们这里
就是主要业务完成以后就是发布事件消息给感兴趣的订阅用户进行后续操作。


### 使用例子

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/")
public class EventController {

    @GetMapping("/event")
    public void sendEvent() {
        // Do some business code here.
        EventBus.getDefault().post("HelloEvent");
    }

    // 订阅感兴趣事件
    @Subscribe
    public void handleEvent(String event) {
        // Do something business code here.
        logger.info("我收到了一个事件: {}", event);
    }
}
```
其中 @Subscribe 注解用于标注在我们需要接受消息的方法上面，其中有三个参数可以我们控制：
- threadMode: 事件派发线程模型;
  - POSTING：当前发布线程进行事件进行处理，这是默认的处理线程模型；
  - ASYNC：将事件的处理交给异步线程池进行处理；
  - CUSTOM：自定义线程池进行处理，这里需要通过指定threadPoolName，程序会通过Spring根据threadPoolName去Spring容器查找该相关线程池；
- threadPoolName: 事件派发的线程池名称，这个通常用于自定义线程模型时用到会从Spring容器中根据名称进行查询自定义的线程池;
- priority：指定事件订阅方法的优先级,默认0,如果多个事件订阅方法可以接收相同事件的,则优先级高的先接收到事件

### TODO
- [x] 支持ASM字节码增强代替反射
- [x] 需要考虑线程派发模式
- [x] 派发优先级问题
- [x] 原生事件消息支持

### 参考
[EventBus详解 (详解 + 原理)](https://blog.csdn.net/m0_49508485/article/details/127780285)

[EventBus](https://gitee.com/nepxion/EventBus)

[matrix](https://gitee.com/nepxion/Matrix/blob/master/matrix-aop/src/main/java/com/nepxion/matrix/selector/AbstractImportSelector.java)