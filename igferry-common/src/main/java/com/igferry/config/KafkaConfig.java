package com.igferry.config;


import java.io.Serializable;

public class KafkaConfig  implements Serializable {


    /**
     *  kafka地址
     */
    private String servers;
    /**
     * 发送队列
     */
    private String producerTopic;
    /**
     * 反馈队列
     */
    private String consumerTopic;
    /**
     *  拉取最长时间
     */
    private int pollTimeout;
    /**
     *  反馈超时时间
     */
    private int timeout;

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public String getProducerTopic() {
        return producerTopic;
    }

    public void setProducerTopic(String producerTopic) {
        this.producerTopic = producerTopic;
    }

    public String getConsumerTopic() {
        return consumerTopic;
    }

    public void setConsumerTopic(String consumerTopic) {
        this.consumerTopic = consumerTopic;
    }

    public int getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(int pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
