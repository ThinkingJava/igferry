# 一款高性能的穿透网闸架构
## 1  前言

​	搞了几个月的某某项目,无尽的人力运维，排查问题，跟踪问题，非常痛苦，基于项目原有的问题，汇总，思考，所以设计了一个技术解决方案。

## 2 设计

​	由于项目处于两个不同局域网，两个局域网之间，有着类似DMZ区域的网络，通讯需要开通相关网络策略及协议，限制比较多。

### 2.1 技术选型

项目重构前，问题排查异常困难，发送消息后，业务往往不清楚是否处理了，于是整理了一下，比如以下场景：

- 业务希望请求可以有一定堆积，保证服务正常运行。
- 业务希望能够请求获得另一端网络应用实时或异步反馈。
- 为了方便问题排查，具有一定的消息查询能力。

#### 2.1.1 使用kafka作为不同网络之间通讯工具

​	选择kafka作为两个不同网络通讯工具好处：

1. Kafka是一个分布式消息队列，具有高性能、持久化、多副本备份，可以起到削峰填谷的作用。高吞吐量、低延迟，解耦两个局域网通讯。
2. 实践检验，它的延迟最低只有几毫秒。

#### 2.1.2 重构网关

一般网关所拥有的功能有， 高并发，请求鉴权，负载均衡，路由转发。

网关是所有请求的入口，所以要求有很高的吞吐量，为了实现这点可以使用请求异步化来解决。目前一般有以下两种方案：

- Tomcat
- Jetty+NIO+Servlet3

Netty为高并发而生，spring5后推出Spring WebFlux（底层Netty）的，对比传统的springmvc性能高出很多，在相同的情况下Netty是每秒30w+的吞吐量，Tomcat是13w+，可以看出是有一定的差距的，网上已经有很多的测评，不再在过多说明。

#### 2.1.3 Nacos注册中心

在微服务架构下，服务都会进行多实例部署来保证高可用，请求到达网关时，网关需要根据URL找到所有可用的实例，这时就需要服务注册和发现功能，即注册中心。

#### 2.1.4 zookpeer使用

生成分布式唯一ID，创建一个持久顺序节点，创建操作返回的节点序号，用于确保每个分发节点拥有独立的反馈队列。

#### 2.1.4 架构设计

将项目划分为以下几个模块

| 名称            | 描述                                                     |
| --------------- | -------------------------------------------------------- |
| igferry-common  | 一些公共的代码，常量，异常类等。                         |
| igferry-server  | 测试应用功能模块                                         |
| igferry-gateway | 网关应用，消费mq投递消息，消息鉴权，负载均衡，路由转发等 |
| igferry-deliver | 把请求转换为mq消息，相当于分发服务                       |

#### 2.1.5 项目架构图

![image](https://github.com/ThinkingJava/igferry/blob/develop/image/架构图.png?raw=true)

## 3 服务说明
### 3.1 igferry-deliver服务

- 通过zookpeer，为每一个igferry-deliver应用节点生成一个分布式id，利用该id生成一个独立的反馈队列。

- 利用响应式编码特点，生成一个唯一id,并发送mq消息，缓存队列中存放该id及Mono对象，同时监听反馈队列消息，反馈监听到该消息后则置为success。

  生成workid核心代码如下：

  ```java
   try {
              CuratorFramework curator = createWithOptions(connectionString, new RetryUntilElapsed(10000, 4), 10000, 6000);
              curator.start();
              Stat stat = curator.checkExists().forPath(PATH_FOREVER);
              if (stat == null) {
                  //不存在根节点,机器第一次启动,创建/ferry/ip:port-000000000,并上传数据
                  zk_AddressNode = createNode(curator);
                  //worker id 默认是0
                  updateLocalWorkerID(workerID);
                  //定时上报本机时间给forever节点
                  ScheduledUploadData(curator, zk_AddressNode);
                  return true;
              } else {
                  Map<String, Integer> nodeMap = Maps.newHashMap();//ip:port->00001
                  Map<String, String> realNode = Maps.newHashMap();//ip:port->(ipport-000001)
                  //存在根节点,先检查是否有属于自己的根节点
                  List<String> keys = curator.getChildren().forPath(PATH_FOREVER);
                  for (String key : keys) {
                      String[] nodeKey = key.split("-");
                      realNode.put(nodeKey[0], key);
                      nodeMap.put(nodeKey[0], Integer.parseInt(nodeKey[1]));
                  }
                  Integer workerid = nodeMap.get(listenAddress);
                  if (workerid != null) {
                      //有自己的节点,zk_AddressNode=ip:port
                      zk_AddressNode = PATH_FOREVER + "/" + realNode.get(listenAddress);
                      workerID = workerid;//启动worder时使用会使用
                      if (!checkInitTimeStamp(curator, zk_AddressNode)) {
                          throw new IGFerryException("2001","init timestamp check error,forever node timestamp gt this node time");
                      }
                      //准备创建临时节点
                      doService(curator);
                      updateLocalWorkerID(workerID);
                      log.info("[Old NODE]find forever node have this endpoint ip-{} port-{} workid-{} childnode and start SUCCESS", ip, port, workerID);
                  } else {
                      //表示新启动的节点,创建持久节点 ,不用check时间
                      String newNode = createNode(curator);
                      zk_AddressNode = newNode;
                      String[] nodeKey = newNode.split("-");
                      workerID = Integer.parseInt(nodeKey[1]);
                      doService(curator);
                      updateLocalWorkerID(workerID);
                      log.info("[New NODE]can not find node on forever node that endpoint ip-{} port-{} workid-{},create own node on forever node and start SUCCESS ", ip, port, workerID);
                  }
              }
          } catch (Exception e) {
              log.error("Start node ERROR {}", e);
              try {
                  Properties properties = new Properties();
                  properties.load(new FileInputStream(new File(PROP_PATH.replace("{port}", port + ""))));
                  workerID = Integer.valueOf(properties.getProperty("workerID"));
                  log.warn("START FAILED ,use local node file properties workerID-{}", workerID);
              } catch (Exception e1) {
                  log.error("Read file error ", e1);
                  return false;
              }
          }
  ```

  发送并监听mq消息,并返回Mono<Response>对象。

  ```java
  		final KafkaConnect connect = KafkaOperation.getKafkaConnect(kafkaConfig);
          final Function callback = new Function() {
              @Override
              public Object apply(final Object t) {
                  connect.startListenerMQReceiveMsg(kafkaConfig, connect);
                  connect.sendKafkaMsg(kafkaConfig, request, key);
                  return null;
              }
          };
          final Mono<Response> resultMono = KafkaOperation.invokeInternal(requestContext, key, kafkaConfig.getTimeout(), callback);
          return resultMono;
  ```

  mq发送消息及反馈响应方法

  ```java
   public static Mono<Response> invokeInternal(final RequestContext context, final String key, final int timeout, final Function callback) {
          return Mono.defer(() -> {
              return getResultMsg(context, key, timeout, callback).flatMap(result -> Mono.just(context.getResponse()));
          });
      }
  
      public static Mono<Boolean> getResultMsg(final RequestContext context, final String key, final int timeout, final Function callback) {
          return Mono.create(sink -> {
              MQCache.addMonoSink(key, sink);
              callback.apply(null);
          }).timeout(Duration.ofSeconds(timeout)).onErrorResume(ex -> {
              if (ex instanceof TimeoutException) {
                  throw new IGFerryException("2001", "Kafka message timeout");
              } else {
                  throw new IGFerryException("2002", "Kafka message error");
              }
          }).map(data -> {
              log.debug("Receive kafka key:[{}] callback msg:[{}]", key, data);
              Response response = JacksonUtil.fromJson((String) data, Response.class);
              context.setResponse(response);
              return true;
          }).doFinally(onFinally -> {
              MQCache.KEY_MONO_SINK.remove(key);
          });
      }
  ```

  接收到mq消息时处理

  ```java
   final String key = record.key();
   try {
  		final MonoSink<Object> monoSink = MQCache.getMonoSink(key);
  		if (monoSink != null) {
  			monoSink.success(value);
  		} else {
          	log.error("Key not found for kafka message, key: [{}], value: [{}]", key, value);
      	}
  	} catch (Exception e) {
       log.error("Handle kafka record error, key: [{}], value: [{}]", new Object[]{key, value, e});
   }
  ```

### 3.2 igferry-gateway网关服务

- 统一监听消费队列，消息拉取后通过路由API转发到具体服务处理

- 实现mq消息负载均衡

- 将消息交给 chain去链式处理,实现消息鉴权和路由分发



监听mq消息统一处理

  ```java
  MASTER_POOL.submit(() -> {
  	try {
  		Request request = JacksonUtil.fromJson(value, Request.class);
  		SpringBeanUtils.getBean(KafkaMessageDynamicLoad.class).loadMQMsg(request).block();
      } catch (Exception e) {
  		e.printStackTrace();
          log.error("handle msg error topic: {}, value: {} ,errormsg: {}", new Object[]{consumerTopic, value, e.getMessage()});
      }
  });
  ```

根据消息API获取服务名,把消息给 chain去链式处理

  ```java
  public Mono<Void> loadMQMsg(Request<?> request){
        String appName = parseAppName(request);
        MQInvokerChain mqInvokerChain = new MQInvokerChain(serverConfigProperties,gatewayKafkaConfig,grayLoadBalancer
                  ,gatewayKafkaConnect,appName);
          mqInvokerChain.addPlugin(new MsgAuthMQInvoker(serverConfigProperties,gatewayKafkaConfig,grayLoadBalancer,gatewayKafkaConnect));
          mqInvokerChain.addPlugin(new MsgDynamicRouteMQInvoker(serverConfigProperties,gatewayKafkaConfig,grayLoadBalancer,gatewayKafkaConnect));
         return mqInvokerChain.execute(request,mqInvokerChain);
  }
  ```



执行调用链实现类。

  ```java
  public class MQInvokerChain extends AbstractMQInvokerImpl {
  
      /**
       *  服务id
       */
      private String appName;
      /**
       * 当前执行的链路插件
       */
      private int pos;
  
      /**
       * 存放服务链路
       */
      private List<MQInvoker> mqInvokers;
  
      public MQInvokerChain(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig,
                            GrayLoadBalancer grayLoadBalancer,
                            GatewayKafkaConnect gatewayKafkaConnect,String appName) {
          super(serverConfigProperties, gatewayKafkaConfig,grayLoadBalancer, gatewayKafkaConnect);
          this.appName = appName;
  
      }
  
      /**
       * 将启用的插件添加到链
       *
       * @param mqInvoker
       */
      public void addPlugin(MQInvoker mqInvoker) {
          if (mqInvokers == null) {
              mqInvokers = new ArrayList<>();
          }
          mqInvokers.add(mqInvoker);
          // 排序
          mqInvokers.sort(Comparator.comparing(MQInvoker::order));
      }
  
      @Override
      public Integer order() {
          return 0;
      }
  
      @Override
      public String name() {
          return null;
      }
  
      /**
       *  执行调用链
       */
      @Override
      public Mono<Void> execute(Request<?> request, MQInvokerChain mqInvokerChain) {
          if (pos == mqInvokers.size()) {
              return Mono.empty();
          }
          return mqInvokerChain.mqInvokers.get(pos++).execute(request, mqInvokerChain);
      }
  
      public String getAppName() {
          return appName;
      }
  }
  ```

动态路由转发及均衡负载实现类

  ```java
  public class MsgDynamicRouteMQInvoker extends AbstractMQInvokerImpl {
  
      private static WebClient webClient;
  
      public MsgDynamicRouteMQInvoker(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig,
                                      GrayLoadBalancer grayLoadBalancer,
                                      GatewayKafkaConnect gatewayKafkaConnect) {
          super(serverConfigProperties, gatewayKafkaConfig, grayLoadBalancer, gatewayKafkaConnect);
      }
  
      static {
          HttpClient httpClient = HttpClient.create()
                  .tcpConfiguration(client ->
                          client.doOnConnected(conn ->
                                  conn.addHandlerLast(new ReadTimeoutHandler(180))
                                          .addHandlerLast(new WriteTimeoutHandler(180)))
                                  .option(ChannelOption.TCP_NODELAY, true)
                  );
  
          webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                  .build();
      }
  
      @Override
      public Integer order() {
          return MQInvokerEnum.DYNAMIC_FERRY_ROUTE.getOrder();
      }
  
      @Override
      public String name() {
          return MQInvokerEnum.DYNAMIC_FERRY_ROUTE.name();
      }
  
      @Override
      public Mono<Void> execute(Request<?> request, MQInvokerChain pluginChain) {
          log.info("mq消息路由转发");
          String appName = pluginChain.getAppName();
          ServiceInstance serviceInstance = chooseInstance(appName);
          String url = buildUrl(serviceInstance, request.getApiUrl());
          return forward(request, url);
      }
  
      private Mono<Void> forward(Request<?> request, String url) {
          HttpMethod httpMethod = HttpMethod.valueOf(request.getHttpMethod());
          WebClient.RequestBodySpec requestBodySpec = webClient.method(httpMethod).uri(url);
  
          WebClient.RequestHeadersSpec<?> reqHeadersSpec;
          if (requireHttpBody(httpMethod)) {
              reqHeadersSpec = requestBodySpec.contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(request.getBody()));
          } else {
              reqHeadersSpec = requestBodySpec;
          }
          // 消息回调
           return getResultMsg(request, reqHeadersSpec).flatMap(response -> {
                  gatewayKafkaConnect.sendCoustomKafka(response.getTopic(), request.getKey(), JacksonUtil.toJson(response));
                  return Mono.empty();
              });
      }
  
      // nio->callback->nio
      private Mono<Response> getResultMsg(Request<?> request, WebClient.RequestHeadersSpec<?> reqHeadersSpec) {
          Mono<Response> responseMono = reqHeadersSpec.exchangeToMono(clientResponse -> {
              return clientResponse.bodyToMono(String.class).flatMap(responseBody -> {
                  Response response = JacksonUtil.fromJson(responseBody, Response.class);
                  response.setTopic(request.getResponseTopic());
                  return Mono.just(response);
              });
          });
          if (request.isAsync()) {
              return responseMono.timeout(Duration.ofMillis(serverConfigProperties.getTimeOutMillis()))
                      .onErrorResume(ex -> {
                          return Mono.defer(() -> {
                              Response response = new Response();
                              response.setTopic(request.getResponseTopic());
                              if (ex instanceof TimeoutException) {
                                  response.setErrCode("5001");
                                  response.setErrMsg("network timeout");
                              } else {
                                  response.setErrCode("5000");
                                  response.setErrMsg("system error");
                              }
                              return Mono.just(response);
                          }).then(Mono.just(new Response(request.getResponseTopic(), "5000", "system error")));
                      });
          }
          return responseMono;
      }
  
      private String buildUrl(ServiceInstance serviceInstance, String apiUrl) {
          String path = apiUrl.replaceFirst("/" + serviceInstance.getServiceId(), "");
          String url = "http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort() + path;
          return url;
      }
  
      /**
       * 查看http请求是否需要参数body
       * @param method
       * @return
       */
      private boolean requireHttpBody(HttpMethod method) {
          if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
              return true;
          }
          return false;
      }
  
      /**
       * 根据路由规则配置和负载均衡算法选择服务实例
       *
       * @param appName
       * @return
       */
      private ServiceInstance chooseInstance(String appName) {
          return grayLoadBalancer.choose(appName);
      }
  
  }
  ```

### 3.3 igferry-server服务

- 该服务为后端具体实现业务的服务类，实现为一个简单的controller接口。





## 4 本地调试

### 4.1 本地启动nacos服务。

从 Github 上下载源码方式

```bash
git clone https://github.com/alibaba/nacos.git
cd nacos/
mvn -Prelease-nacos -Dmaven.test.skip=true clean install -U  
ls -al distribution/target/

// change the $version to your actual path
cd distribution/target/nacos-server-$version/nacos/bin
//启动nacos
startup.cmd -m standalone
```

### 4.2 启动zookeeper和kafka

下载zookpeer压缩包 [zookpeer]( https://mirrors.tuna.tsinghua.edu.cn/apache/zookeeper/zookeeper-3.6.2/apache-zookeeper-3.6.2-bin.tar.gz)
，解压放到D盘，复制D:\apache-zookeeper-3.6.2-bin\conf\zoo_sample.cfg并修改为zoo.cfg，cmd窗口下启动
D:\apache-zookeeper-3.6.2-bin\bin\zkServer.cmd

下载kafka压缩包 [kafka]( http://archive.apache.org/dist/kafka/2.6.0/kafka_2.12-2.6.0.tgz )

，解压放到D盘，cmd窗口下启动

D:\kafka_2.12-2.6.0\bin\windows\kafka-server-start.bat D:\kafka_2.12-2.6.0\config\server.properties

启动kafka后为topic配置6个分区

D:\kafka_2.12-2.6.0\bin\windows\kafka-topics.bat --alter --zookeeper localhost:2181 --partitions 6 --topic igferry-gateway-deliver

### 4.3 启动两个igferry-gateway

实例1配置： 在启动参数VM options 添加 -Dserver.port=9999

实例2配置：在启动参数VM options 添加 -Dserver.port=9998

### 4.4 启动两个igferry-server

实例1配置： 在启动参数VM options 添加 -Dserver.port=4001

实例2配置：在启动参数VM options 添加 -Dserver.port=4002

### 4.5 启动igferry-deliver



### 4.6 用postman调试链路是否正常

![image](https://github.com/ThinkingJava/igferry/blob/develop/image/postman请求.png?raw=true)

### 4.7 性能压测

压测环境：

联想小新15 2021AMD版

处理器 1.8 GHz 八核AMD Ryzen7 4800U

内存 16 GB

网关和后端应用两个，分发服务一个

压测工具：JMeter

压测结果：1000个线程，循环10次，吞吐量大概每秒1657个请求。

![image](https://raw.githubusercontent.com/ThinkingJava/igferry/develop/image/压测1.png?raw=true)

![image](https://raw.githubusercontent.com/ThinkingJava/igferry/develop/image/压测2.png?raw=true)

![image](https://github.com/ThinkingJava/igferry/blob/develop/image/压测3.png?raw=true)



## 5.总结

构思了很久一段时间并没有赋予实际。开始的确感觉非常困难，但当实际开始行动时就会发现其实没那么难，所以迈出第一步很重要。

过程中遇到很多的困难和问题，也参考了一些优秀的文章解决问题。要归纳整理自己的知识库，

#### 参考资料

https://cnblogs.com/2YSP/p/14223892.html

https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/web-reactive.html#webflux

https://blog.csdn.net/bskfnvjtlyzmv867/article/details/90247036

