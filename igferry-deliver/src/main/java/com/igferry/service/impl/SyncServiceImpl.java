package com.igferry.service.impl;

import com.igferry.config.DeliverKafkaConfig;
import com.igferry.kafka.connect.KafkaConnect;
import com.igferry.kafka.context.Request;
import com.igferry.kafka.context.RequestContext;
import com.igferry.kafka.context.Response;
import com.igferry.kafka.util.KafkaOperation;
import com.igferry.pojo.qo.RequestQO;
import com.igferry.service.SyncService;
import com.igferry.zookeeper.id.WorkerIdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.function.Function;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: 同步请求服务类
 */
@Service
public class SyncServiceImpl implements SyncService {

    @Resource
    private DeliverKafkaConfig kafkaConfig;


    @Override
    public Mono<Response> syncRequest(RequestQO requestQO,String apiUrl,String method) {
        String key = UUID.randomUUID().toString();
        RequestContext requestContext = new RequestContext();
        Request<RequestQO> request = new Request();
        request.setAsync(true);
        request.setKey(key);
        request.setHttpMethod(method);
        request.setApiUrl(apiUrl);
        request.setSendTopic(kafkaConfig.getProducerTopic());
        request.setResponseTopic(kafkaConfig.getConsumerTopic());
        request.setBody(requestQO);
        requestContext.setRequest(request);
        final KafkaConnect connect = KafkaOperation.getKafkaConnect(kafkaConfig);
        final Function callback = new Function() {
            @Override
            public Object apply(final Object t) {
                connect.startListenerMQReceiveMsg(kafkaConfig, connect);
                connect.sendKafkaMsg(kafkaConfig, request, key);
                return null;
            }
        };
        final Mono<Response> resultMono = KafkaOperation.invokeInternal(requestContext, key, kafkaConfig.getTimeout(), callback);
        return resultMono;
    }
}
