package com.igferry.invoker.impl;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.config.ServerConfigProperties;
import com.igferry.enums.MQInvokerEnum;
import com.igferry.invoker.MQInvokerChain;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import com.igferry.kafka.context.Request;
import com.igferry.kafka.context.Response;
import com.igferry.rule.GrayLoadBalancer;
import com.igferry.util.JacksonUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
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
