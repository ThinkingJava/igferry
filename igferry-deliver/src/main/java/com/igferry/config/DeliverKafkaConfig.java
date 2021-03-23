package com.igferry.config;

import com.igferry.zookeeper.ZookeeperConfiguration;
import com.igferry.zookeeper.id.WorkerIdGenerator;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: kafka配置
 */
@Data
@Component
@AutoConfigureAfter({ZookeeperConfiguration.class})
@ConfigurationProperties(prefix = "igferry.kafka")
public class DeliverKafkaConfig extends KafkaConfig {

    @Autowired
    private WorkerIdGenerator workerIdGenerator;

    /**
     * 响应队列
     */
    public String getConsumerTopic() {
        return super.getConsumerTopic().concat(String.valueOf(workerIdGenerator.getWorkId()));
    }

}
