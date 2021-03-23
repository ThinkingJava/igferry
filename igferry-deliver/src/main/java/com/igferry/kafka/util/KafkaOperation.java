package com.igferry.kafka.util;

import com.igferry.config.DeliverKafkaConfig;
import com.igferry.exception.IGFerryException;
import com.igferry.kafka.cache.MQCache;
import com.igferry.kafka.connect.KafkaConnect;
import com.igferry.kafka.context.RequestContext;
import com.igferry.kafka.context.Response;
import com.igferry.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: kafka发送及反馈响应类
 */
@Slf4j
public class KafkaOperation {

    private static KafkaConnect kafkaConnect;

    public static Mono<Response> invokeInternal(final RequestContext context, final String key, final int timeout, final Function callback) {
        return Mono.defer(() -> {
            return getResultMsg(context, key, timeout, callback).flatMap(result -> Mono.just(context.getResponse()));
        });
    }

    public static Mono<Boolean> getResultMsg(final RequestContext context, final String key, final int timeout, final Function callback) {
        return Mono.create(sink -> {
            MQCache.addMonoSink(key, sink);
            callback.apply(null);
        }).timeout(Duration.ofSeconds(timeout)).onErrorResume(ex -> {
            if (ex instanceof TimeoutException) {
                throw new IGFerryException("2001", "Kafka message timeout");
            } else {
                throw new IGFerryException("2002", "Kafka message error");
            }
        }).map(data -> {
            log.debug("Receive kafka key:[{}] callback msg:[{}]", key, data);
            Response response = JacksonUtil.fromJson((String) data, Response.class);
            context.setResponse(response);
            return true;
        }).doFinally(onFinally -> {
            MQCache.KEY_MONO_SINK.remove(key);
        });
    }

    public static KafkaConnect getKafkaConnect(final DeliverKafkaConfig kafkaConfig) {
        synchronized (KafkaOperation.class) {
            if (kafkaConnect == null) {
                kafkaConnect = new KafkaConnect(kafkaConfig);
            }
        }
        return kafkaConnect;
    }

}
