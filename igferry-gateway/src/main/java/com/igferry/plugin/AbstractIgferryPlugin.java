package com.igferry.plugin;

import com.igferry.rule.GrayLoadBalancer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13 11:12
 *  @Description:
 */
public abstract class AbstractIgferryPlugin implements IgferryPlugin {

    private GatewayLoadBalancerProperties loadBalancerProperties;

    private LoadBalancerProperties properties;

    private GrayLoadBalancer grayLoadBalancer;


    public AbstractIgferryPlugin(GatewayLoadBalancerProperties loadBalancerProperties,
                                 LoadBalancerProperties properties, GrayLoadBalancer grayLoadBalancer) {
        this.properties = properties;
        this.grayLoadBalancer = grayLoadBalancer;
        this.loadBalancerProperties = loadBalancerProperties;
    }
}
