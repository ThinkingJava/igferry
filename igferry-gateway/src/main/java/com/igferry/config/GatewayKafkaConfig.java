package com.igferry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "igferry.kafka")
public class GatewayKafkaConfig extends KafkaConfig{



}
