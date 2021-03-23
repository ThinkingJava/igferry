package com.igferry.plugin;


import com.igferry.chain.PluginChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: Igferry调用链
 */
public interface IgferryPlugin {
    /**
     * lower values have higher priority
     *
     * @return
     */
    Integer order();

    /**
     * return current plugin name
     *
     * @return
     */
    String name();

    Mono<Void> execute(ServerWebExchange exchange,PluginChain pluginChain);

}
