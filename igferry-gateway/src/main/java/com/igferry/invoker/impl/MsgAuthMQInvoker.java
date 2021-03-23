package com.igferry.invoker.impl;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.config.ServerConfigProperties;
import com.igferry.enums.MQInvokerEnum;
import com.igferry.invoker.MQInvokerChain;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import com.igferry.kafka.context.Request;
import com.igferry.rule.GrayLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class MsgAuthMQInvoker extends AbstractMQInvokerImpl {

    public MsgAuthMQInvoker(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig,
                            GrayLoadBalancer grayLoadBalancer,
                            GatewayKafkaConnect gatewayKafkaConnect) {
        super(serverConfigProperties, gatewayKafkaConfig,grayLoadBalancer, gatewayKafkaConnect);
    }

    @Override
    public Integer order() {
        return MQInvokerEnum.AUTH.getOrder();
    }

    @Override
    public String name() {
        return MQInvokerEnum.DYNAMIC_FERRY_ROUTE.name();
    }

    @Override
    public Mono<Void> execute(Request<?> request, MQInvokerChain pluginChain) {
        log.info("mq消息鉴权");
        return pluginChain.execute(request,pluginChain);
    }
}
