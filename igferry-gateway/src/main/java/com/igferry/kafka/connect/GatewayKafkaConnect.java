package com.igferry.kafka.connect;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.kafka.receive.ReceiveGatewayListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@Slf4j
public class GatewayKafkaConnect {

    private Producer producer;
    private Consumer kafkaConsumer;
    private volatile boolean isRunning;

    public Producer getProducer() {
        return this.producer;
    }

    public Consumer getKafkaConsumer() {
        return this.kafkaConsumer;
    }

    public GatewayKafkaConnect() {
        this.isRunning = false;
    }

    public void initKafkaProducer(GatewayKafkaConfig kafkaConfig){
        if (this.producer == null) {
            this.producer = new KafkaProducer(this.getProducerConfig(kafkaConfig));
        }
    }

    public void initKafkaConsumer(GatewayKafkaConfig kafkaConfig){
        if (this.kafkaConsumer == null) {
            this.kafkaConsumer = new KafkaConsumer(this.getConsumerConfig(kafkaConfig));
        }
    }

    public void initKafkaConnect(final GatewayKafkaConfig kafkaConfig) {
        if (this.producer == null) {
            this.producer = new KafkaProducer(this.getProducerConfig(kafkaConfig));
        }
        if (this.kafkaConsumer == null) {
            this.kafkaConsumer = new KafkaConsumer(this.getConsumerConfig(kafkaConfig));
        }
    }

    private Properties getProducerConfig(final GatewayKafkaConfig kafkaConfig) {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConfig.getServers()); // 设置 Broker 的地址
        props.put("acks", "1"); // 0-不应答。1-leader 应答。all-所有 leader 和 follower 应答。
        props.put("retries", 3); // 发送失败时，重试发送的次数
        props.put("key.serializer", StringSerializer.class.getName()); // 消息的 key 的序列化方式
        props.put("value.serializer", StringSerializer.class.getName()); // 消息的 value 的序列化方式
        return props;
    }

    private Properties getConsumerConfig(final GatewayKafkaConfig kafkaConfig) {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConfig.getServers()); // 设置 Broker 的地址
        props.put("max.poll.records", 100);
        props.put("max.poll.interval.ms", 60000);
        props.put("group.id", "check-consumer-group"); // 消费者分组
        props.put("auto.offset.reset", "latest"); // 设置消费者分组最初的消费进度为 earliest 。
        props.put("enable.auto.commit", true); // 是否自动提交消费进度
        props.put("auto.commit.interval.ms", "1000"); // 自动提交消费进度频率
        props.put("key.deserializer", StringDeserializer.class.getName()); // 消息的 key 的反序列化方式
        props.put("value.deserializer", StringDeserializer.class.getName()); // 消息的 value 的反序列化方式
        return props;
    }

    public synchronized void startListenerMQReceiveMsg(final GatewayKafkaConfig kafkaConfig) {
        if (!this.isRunning) {
            final Thread listenerThread = new Thread(new ReceiveGatewayListener(this.getKafkaConsumer(), kafkaConfig));
            listenerThread.setName(kafkaConfig.getConsumerTopic() + "-listener");
            listenerThread.start();
            this.isRunning = true;
        }
    }


    public void sendCoustomKafka(final String sendTopic, final String sendKey, final String value) {
        this.getProducer().send(new ProducerRecord(sendTopic, sendKey, value), (metadata, exception) -> {
            if (exception != null) {
                log.error("Send topic [{}] key [{}] kafka message error, request data: {} ",sendTopic, sendKey, value);
            } else {
                log.info("Send topic [{}] key [{}] kafka message success: {}",sendTopic, sendKey, value);
            }
        });
    }

}
