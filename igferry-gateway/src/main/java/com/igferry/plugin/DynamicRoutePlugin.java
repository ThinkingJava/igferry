package com.igferry.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.igferry.chain.PluginChain;
import com.igferry.chain.ShipResponseUtil;
import com.igferry.config.ServerConfigProperties;
import com.igferry.constants.IgferryPluginEnum;
import com.igferry.rule.GrayLoadBalancer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13 11:12
 *  @Description: 路由转发
 */
@Slf4j
public class DynamicRoutePlugin extends AbstractIgferryPlugin {

    private static WebClient webClient;

    private static final Gson gson = new GsonBuilder().create();

    private GatewayLoadBalancerProperties loadBalancerProperties;

    private LoadBalancerProperties properties;

    private GrayLoadBalancer grayLoadBalancer;

    private ServerConfigProperties serverConfigProperties;

    static {
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(client ->
                        client.doOnConnected(conn ->
                                conn.addHandlerLast(new ReadTimeoutHandler(3))
                                        .addHandlerLast(new WriteTimeoutHandler(3)))
                                .option(ChannelOption.TCP_NODELAY, true)
                );
        webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public DynamicRoutePlugin(GatewayLoadBalancerProperties loadBalancerProperties,
                              LoadBalancerProperties properties, GrayLoadBalancer grayLoadBalancer, ServerConfigProperties serverConfigProperties) {
        super(loadBalancerProperties,properties,grayLoadBalancer);
        this.loadBalancerProperties = loadBalancerProperties;
        this.properties = properties;
        this.grayLoadBalancer = grayLoadBalancer;
        this.serverConfigProperties = serverConfigProperties;
    }

    @Override
    public Integer order() {
        return IgferryPluginEnum.DYNAMIC_ROUTE.getOrder();
    }

    @Override
    public String name() {
        return IgferryPluginEnum.DYNAMIC_ROUTE.getName();
    }

    @Override
    public Mono<Void> execute(ServerWebExchange exchange, PluginChain pluginChain) {
        String appName = pluginChain.getAppName();
        ServiceInstance serviceInstance = chooseInstance(appName);
        // get request url
        String url = buildUrl(exchange, serviceInstance);
        return forward(exchange, url);
    }

    /**
     * forward request to backend service
     *
     * @param exchange
     * @param url
     * @return
     */
    private Mono<Void> forward(ServerWebExchange exchange, String url) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpMethod method = request.getMethod();

        WebClient.RequestBodySpec requestBodySpec = webClient.method(method).uri(url).headers((headers) -> {
            headers.addAll(request.getHeaders());
        });

        WebClient.RequestHeadersSpec<?> reqHeadersSpec;
        if (requireHttpBody(method)) {
            reqHeadersSpec = requestBodySpec.body(BodyInserters.fromDataBuffers(request.getBody()));
        } else {
            reqHeadersSpec = requestBodySpec;
        }

        // nio->callback->nio
        return reqHeadersSpec.exchange().timeout(Duration.ofMillis(serverConfigProperties.getTimeOutMillis()))
                .onErrorResume(ex -> {
                    return Mono.defer(() -> {
                        String errorResultJson = "";
                        if (ex instanceof TimeoutException) {
                            errorResultJson = "{\"code\":5001,\"message\":\"network timeout\"}";
                        } else {
                            errorResultJson = "{\"code\":5000,\"message\":\"system error\"}";
                        }
                        return ShipResponseUtil.doResponse(exchange, errorResultJson);
                    }).then(Mono.empty());
                }).flatMap(backendResponse -> {
                    response.setStatusCode(backendResponse.statusCode());
                    response.getHeaders().putAll(backendResponse.headers().asHttpHeaders());
                    return response.writeWith(backendResponse.bodyToFlux(DataBuffer.class));
                });
    }

    /**
     * weather the http method need http body
     *
     * @param method
     * @return
     */
    private boolean requireHttpBody(HttpMethod method) {
        if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
            return true;
        }
        return false;
    }

    private String buildUrl(ServerWebExchange exchange, ServiceInstance serviceInstance) {
        ServerHttpRequest request = exchange.getRequest();
        String query = request.getURI().getQuery();
        String path = request.getPath().value().replaceFirst("/" + serviceInstance.getServiceId(), "");
        String url = "http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort() + path;
        if (!StringUtils.isEmpty(query)) {
            url = url + "?" + query;
        }
        return url;
    }

    /**
     * choose an ServiceInstance according to route rule config and load balancing algorithm
     *
     * @param appName
     * @return
     */
    private ServiceInstance chooseInstance(String appName) {
        return grayLoadBalancer.choose(appName);
    }


}
