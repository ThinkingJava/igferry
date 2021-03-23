package com.igferry.filter;

import com.igferry.chain.PluginChain;
import com.igferry.config.ServerConfigProperties;
import com.igferry.plugin.AuthPlugin;
import com.igferry.plugin.DynamicRoutePlugin;
import com.igferry.rule.GrayLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
public class GrayReactiveLoadBalancerClientFilter extends ReactiveLoadBalancerClientFilter {
    private static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150;

    private GatewayLoadBalancerProperties loadBalancerProperties;

    private LoadBalancerProperties properties;

    private GrayLoadBalancer grayLoadBalancer;

    private ServerConfigProperties serverConfigProperties;

    public GrayReactiveLoadBalancerClientFilter(GatewayLoadBalancerProperties loadBalancerProperties,
                                                LoadBalancerProperties properties, GrayLoadBalancer grayLoadBalancer,ServerConfigProperties serverConfigProperties) {
        super(null, loadBalancerProperties, properties);
        this.properties = properties;
        this.grayLoadBalancer = grayLoadBalancer;
        this.loadBalancerProperties = loadBalancerProperties;
        this.serverConfigProperties = serverConfigProperties;
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
            return chain.filter(exchange);
        }
        String appName = parseAppName(exchange);
        PluginChain pluginChain = new PluginChain(loadBalancerProperties, properties,grayLoadBalancer,appName);
        pluginChain.addPlugin(new DynamicRoutePlugin(loadBalancerProperties, properties,grayLoadBalancer,serverConfigProperties));
        pluginChain.addPlugin(new AuthPlugin(loadBalancerProperties, properties,grayLoadBalancer));
        return pluginChain.execute(exchange, pluginChain);
    }

    private String parseAppName(ServerWebExchange exchange) {
        RequestPath path = exchange.getRequest().getPath();
        String appName = path.value().split("/")[1];
        return appName;
    }

}
