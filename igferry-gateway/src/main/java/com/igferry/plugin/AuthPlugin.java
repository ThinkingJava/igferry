package com.igferry.plugin;

import com.igferry.chain.PluginChain;
import com.igferry.constants.IgferryPluginEnum;
import com.igferry.rule.GrayLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: 请求鉴权
 */
@Slf4j
public class AuthPlugin extends AbstractIgferryPlugin {

    public AuthPlugin(GatewayLoadBalancerProperties loadBalancerProperties,
                      LoadBalancerProperties properties, GrayLoadBalancer grayLoadBalancer) {
        super(loadBalancerProperties,properties,grayLoadBalancer);
    }

    @Override
    public Integer order() {
        return IgferryPluginEnum.AUTH.getOrder();
    }

    @Override
    public String name() {
        return IgferryPluginEnum.AUTH.getName();
    }

    @Override
    public Mono<Void> execute(ServerWebExchange exchange, PluginChain pluginChain) {
        log.debug("auth plugin");
        return pluginChain.execute(exchange, pluginChain);
    }
}
