package com.igferry.kafka.receive;


import com.igferry.config.DeliverKafkaConfig;
import com.igferry.kafka.cache.MQCache;
import com.igferry.kafka.connect.KafkaConnect;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ReceiveMsgListener implements Runnable {

    private Consumer consumer;
    private KafkaConnect kafkaConnect;
    private DeliverKafkaConfig kafkaConfig;

    public ReceiveMsgListener(final Consumer consumer, final DeliverKafkaConfig kafkaConfig, final KafkaConnect kafkaConnect) {
        this.kafkaConfig = kafkaConfig;
        this.kafkaConnect = kafkaConnect;
        this.consumer = consumer;
    }


    @Override
    public void run() {
        final String consumerTopic = this.kafkaConfig.getConsumerTopic();
        this.consumer.subscribe(Arrays.asList(consumerTopic.split(";")), new ConsumerRebalanceListener() {
            public void onPartitionsRevoked(final Collection<TopicPartition> arg0) {
                log.info("PartitionsRevoked: {}", arg0);
            }
            public void onPartitionsAssigned(final Collection<TopicPartition> arg0) {
                log.info("PartitionsAssigned: {}", arg0);
            }
        });
        boolean reportPollInfo = true;
        long lastReportTime = System.currentTimeMillis();
        long lastPollTime = System.currentTimeMillis();
        int lastPollRecords = 0;
        while (true) {
            if (reportPollInfo) {
                log.info("Poll start for topic [{}], last handle [{}] records use time [{}]ms.", new Object[]{consumerTopic, lastPollRecords, System.currentTimeMillis() - lastPollTime});
            }
            try {
                final ConsumerRecords<String, String> records = (ConsumerRecords<String, String>) this.consumer.poll(Duration.ofSeconds(this.kafkaConfig.getPollTimeout()));
                lastPollTime = System.currentTimeMillis();
                lastPollRecords = records.count();
                if (lastPollRecords > 0 || System.currentTimeMillis() - lastReportTime >= 60000L) {
                    reportPollInfo = true;
                    lastReportTime = System.currentTimeMillis();
                    log.info("Poll end for topic [{}], [{}] records.", consumerTopic, records.count());
                } else {
                    reportPollInfo = false;
                }
                for (final ConsumerRecord<String, String> record : records) {
                    final String key = record.key();
                    final String value = record.value();
                    log.debug("Receive kafka msg,topic:[{}],key:[{}],record timestamp:[{}],record value:[{}],record offset:[{}],current time:[{}]", new Object[]{record.topic(), key, record.timestamp(), value, record.offset(), System.currentTimeMillis()});
                    try {
                        final MonoSink<Object> monoSink = MQCache.getMonoSink(key);
                        if (monoSink != null) {
                            log.info("Receive kafka callback msg offset:[{}], key:[{}], value:[{}]", new Object[]{record.offset(), key, value});
                            log.debug("Start to set result for key: [{}]", key);
                            monoSink.success(value);
                            log.debug("End to set result for key: [{}]", key);
                        } else {
                            log.error("Key not found for kafka message, key: [{}], value: [{}]", key, value);
                        }
                        log.debug("Done for handling kafka message, key: [{}], value: [{}]", key, value);

                    } catch (Exception e) {
                        log.error("Handle kafka record error, key: [{}], value: [{}]", new Object[]{key, value, e});
                    }
                }
            } catch (WakeupException e2) {
                log.warn("Consumer is wakeup\uff1a{}", e2.getMessage(), e2);
            } catch (Exception e3) {
                log.error("Poll error for topic: {}, message: {}", new Object[]{consumerTopic, e3.getMessage(), e3});
                try {
                    TimeUnit.SECONDS.sleep(3L);
                } catch (InterruptedException e4) {
                    log.error(e3.getMessage(), e3);
                }
            }
        }
    }
}
