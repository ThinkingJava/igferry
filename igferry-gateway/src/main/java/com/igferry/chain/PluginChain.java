package com.igferry.chain;

import com.igferry.plugin.AbstractIgferryPlugin;
import com.igferry.plugin.IgferryPlugin;
import com.igferry.rule.GrayLoadBalancer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description:
 */
public class PluginChain extends AbstractIgferryPlugin {

    private String appName;
    /**
     * the pos point to current plugin
     */
    private int pos;
    /**
     * the plugins of chain
     */
    private List<IgferryPlugin> plugins;

    public PluginChain(GatewayLoadBalancerProperties loadBalancerProperties,
                       LoadBalancerProperties properties, GrayLoadBalancer grayLoadBalancer,String appName) {
        super(loadBalancerProperties,properties,grayLoadBalancer);
        this.appName = appName;
    }

    /**
     * add enabled plugin to chain
     *
     * @param igferryPlugin
     */
    public void addPlugin(IgferryPlugin igferryPlugin) {
        if (plugins == null) {
            plugins = new ArrayList<>();
        }
        plugins.add(igferryPlugin);
        // order by the plugin's order
        plugins.sort(Comparator.comparing(IgferryPlugin::order));
    }

    @Override
    public Integer order() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public Mono<Void> execute(ServerWebExchange exchange, PluginChain pluginChain) {
        if (pos == plugins.size()) {
            return exchange.getResponse().setComplete();
        }
        return pluginChain.plugins.get(pos++).execute(exchange, pluginChain);
    }

    public String getAppName() {
        return appName;
    }

}
