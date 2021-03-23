package com.igferry.invoker.impl;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.config.ServerConfigProperties;
import com.igferry.invoker.MQInvoker;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import com.igferry.rule.GrayLoadBalancer;

public abstract class AbstractMQInvokerImpl implements MQInvoker {

    public ServerConfigProperties serverConfigProperties;

    public GatewayKafkaConfig gatewayKafkaConfig;

    public GatewayKafkaConnect gatewayKafkaConnect;

    public GrayLoadBalancer grayLoadBalancer;


    public AbstractMQInvokerImpl(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig,
                                 GrayLoadBalancer grayLoadBalancer,
                                 GatewayKafkaConnect gatewayKafkaConnect) {
        this.serverConfigProperties = serverConfigProperties;
        this.gatewayKafkaConfig = gatewayKafkaConfig;
        this.grayLoadBalancer = grayLoadBalancer;
        this.gatewayKafkaConnect = gatewayKafkaConnect;
    }

}
