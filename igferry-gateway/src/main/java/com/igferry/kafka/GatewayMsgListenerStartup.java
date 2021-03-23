package com.igferry.kafka;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.configuration.GrayLoadBalancerClientConfiguration;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/14
 *  @Description: 开启消息监听
 */
@Component
@Slf4j
public class GatewayMsgListenerStartup implements ApplicationListener<ApplicationReadyEvent> {


    @Autowired
    private GatewayKafkaConfig gatewayKafkaConfig;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        GatewayKafkaConnect gatewayKafkaConnect = new GatewayKafkaConnect();
        gatewayKafkaConnect.initKafkaConsumer(gatewayKafkaConfig);
        gatewayKafkaConnect.startListenerMQReceiveMsg(gatewayKafkaConfig);
    }
}
