package com.igferry.kafka.load;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.config.ServerConfigProperties;
import com.igferry.invoker.MQInvokerChain;
import com.igferry.invoker.impl.MsgAuthMQInvoker;
import com.igferry.invoker.impl.MsgDynamicRouteMQInvoker;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import com.igferry.kafka.context.Request;
import com.igferry.kafka.context.Response;
import com.igferry.rule.GrayLoadBalancer;
import com.igferry.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Slf4j
public class KafkaMessageDynamicLoad {

    private ServerConfigProperties serverConfigProperties;

    private GatewayKafkaConfig gatewayKafkaConfig;

    private GrayLoadBalancer grayLoadBalancer;

    private GatewayKafkaConnect gatewayKafkaConnect;

    public KafkaMessageDynamicLoad(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig, GrayLoadBalancer grayLoadBalancer) {
        this.serverConfigProperties = serverConfigProperties;
        this.gatewayKafkaConfig = gatewayKafkaConfig;
        this.grayLoadBalancer = grayLoadBalancer;
        this.gatewayKafkaConnect = new GatewayKafkaConnect();
        gatewayKafkaConnect.initKafkaProducer(gatewayKafkaConfig);
    }

    /**
     * @author: yangchenghuan
     * @Date: 2021/3/16
     * @Description: mq处理链
     */
    public Mono<Void> loadMQMsg(Request<?> request) {
        try {
            String appName = parseAppName(request);
            MQInvokerChain mqInvokerChain = new MQInvokerChain(serverConfigProperties, gatewayKafkaConfig, grayLoadBalancer
                    , gatewayKafkaConnect, appName);
            mqInvokerChain.addPlugin(new MsgAuthMQInvoker(serverConfigProperties, gatewayKafkaConfig, grayLoadBalancer, gatewayKafkaConnect));
            mqInvokerChain.addPlugin(new MsgDynamicRouteMQInvoker(serverConfigProperties, gatewayKafkaConfig, grayLoadBalancer, gatewayKafkaConnect));
            return mqInvokerChain.execute(request, mqInvokerChain);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("handle msg error topic: {}, value: {} ,errormsg: {}", new Object[]{request.getSendTopic(), JacksonUtil.toJson(request), e.getMessage()});
            Response response = new Response();
            response.setTopic(request.getResponseTopic());
            response.setErrCode("5000");
            response.setErrMsg(e.getMessage());
            gatewayKafkaConnect.sendCoustomKafka(request.getResponseTopic(), request.getKey(), JacksonUtil.toJson(response));
        }
        return Mono.empty();
    }

    private String parseAppName(Request<?> request) {
        return request.getApiUrl().split("/")[1];
    }

}
