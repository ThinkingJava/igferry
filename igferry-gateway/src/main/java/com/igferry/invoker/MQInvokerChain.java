package com.igferry.invoker;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.config.ServerConfigProperties;
import com.igferry.invoker.impl.AbstractMQInvokerImpl;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import com.igferry.kafka.context.Request;
import com.igferry.rule.GrayLoadBalancer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MQInvokerChain extends AbstractMQInvokerImpl {

    /**
     *  服务id
     */
    private String appName;
    /**
     * 当前执行的链路插件
     */
    private int pos;

    /**
     * 存放服务链路
     */
    private List<MQInvoker> mqInvokers;

    public MQInvokerChain(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig,
                          GrayLoadBalancer grayLoadBalancer,
                          GatewayKafkaConnect gatewayKafkaConnect,String appName) {
        super(serverConfigProperties, gatewayKafkaConfig,grayLoadBalancer, gatewayKafkaConnect);
        this.appName = appName;

    }

    /**
     * 将启用的插件添加到链
     *
     * @param mqInvoker
     */
    public void addPlugin(MQInvoker mqInvoker) {
        if (mqInvokers == null) {
            mqInvokers = new ArrayList<>();
        }
        mqInvokers.add(mqInvoker);
        // 排序
        mqInvokers.sort(Comparator.comparing(MQInvoker::order));
    }

    @Override
    public Integer order() {
        return 0;
    }

    @Override
    public String name() {
        return null;
    }

    /**
     *  执行调用链
     */
    @Override
    public Mono<Void> execute(Request<?> request, MQInvokerChain mqInvokerChain) {
        if (pos == mqInvokers.size()) {
            return Mono.empty();
        }
        return mqInvokerChain.mqInvokers.get(pos++).execute(request, mqInvokerChain);
    }

    public String getAppName() {
        return appName;
    }
}
