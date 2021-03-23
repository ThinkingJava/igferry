package com.igferry.kafka.receive;

import com.igferry.config.GatewayKafkaConfig;
import com.igferry.kafka.connect.GatewayKafkaConnect;
import com.igferry.kafka.context.Request;
import com.igferry.kafka.load.KafkaMessageDynamicLoad;
import com.igferry.util.JacksonUtil;
import com.igferry.util.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;

@Slf4j
public class ReceiveGatewayListener implements Runnable {

    private Consumer consumer;
    private GatewayKafkaConfig kafkaConfig;

    private static final ExecutorService MASTER_POOL;
    private static final ExecutorService SECONDARY_POOL;

    public ReceiveGatewayListener(final Consumer consumer, final GatewayKafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
        this.consumer = consumer;
    }

    @PreDestroy
    public static void shutdown() {
        ReceiveGatewayListener.SECONDARY_POOL.shutdown();
        ReceiveGatewayListener.MASTER_POOL.shutdown();
    }

    static {
        SECONDARY_POOL = new ThreadPoolExecutor(10, 50, 20L, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
        MASTER_POOL = new ThreadPoolExecutor(20, 100, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
                log.debug(String.format("主线程池已满%s，交由副线程池执行%s", executor.toString(), ReceiveGatewayListener.SECONDARY_POOL.toString()));
                ReceiveGatewayListener.SECONDARY_POOL.submit(r);
            }
        });
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
                    String key = record.key();
                    String value = record.value();
                    log.debug("Receive kafka msg,topic:[{}],key:[{}],record timestamp:[{}],record value:[{}],record offset:[{}],current time:[{}]", new Object[]{record.topic(), key, record.timestamp(), value, record.offset(), System.currentTimeMillis()});
                    MASTER_POOL.submit(() -> {
                        Request request = JacksonUtil.fromJson(value, Request.class);
                        SpringBeanUtils.getBean(KafkaMessageDynamicLoad.class).loadMQMsg(request).block();
                    });
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
