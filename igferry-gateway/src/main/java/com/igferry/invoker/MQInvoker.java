package com.igferry.invoker;

import com.igferry.chain.PluginChain;
import com.igferry.kafka.context.Request;
import reactor.core.publisher.Mono;

public interface MQInvoker {

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

    /**
     * request this
     *
     * @return
     */
    Mono<Void> execute(Request<?> request,MQInvokerChain pluginChain);


}
