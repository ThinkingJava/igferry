package com.igferry.configuration;


import com.igferry.config.GatewayKafkaConfig;
import com.igferry.config.ServerConfigProperties;
import com.igferry.filter.GrayReactiveLoadBalancerClientFilter;
import com.igferry.kafka.load.KafkaMessageDynamicLoad;
import com.igferry.rule.GrayLoadBalancer;
import com.igferry.rule.VersionGrayLoadBalancer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mica ribbon rule auto configuration.
 *
 * @author L.cm
 * @link https://github.com/lets-mica/mica
 */
@Configuration
@EnableConfigurationProperties(LoadBalancerProperties.class)
@ConditionalOnProperty(value = "gray.rule.enabled", havingValue = "true")
@AutoConfigureBefore(GatewayReactiveLoadBalancerClientAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class GrayLoadBalancerClientConfiguration {

    @Bean
    public ReactiveLoadBalancerClientFilter gatewayLoadBalancerClientFilter(GrayLoadBalancer grayLoadBalancer,
                                                                            LoadBalancerProperties properties, GatewayLoadBalancerProperties loadBalancerProperties, ServerConfigProperties serverConfigProperties) {
        return new GrayReactiveLoadBalancerClientFilter(loadBalancerProperties, properties, grayLoadBalancer,serverConfigProperties);
    }

    @Bean
    public GrayLoadBalancer grayLoadBalancer(ServerConfigProperties serverConfigProperties, DiscoveryClient discoveryClient) {
        return new VersionGrayLoadBalancer(serverConfigProperties,discoveryClient);
    }

    @Bean
    public KafkaMessageDynamicLoad kafkaMessageDynamicLoad(ServerConfigProperties serverConfigProperties, GatewayKafkaConfig gatewayKafkaConfig,
                                                          GrayLoadBalancer grayLoadBalancer){
        return new KafkaMessageDynamicLoad(serverConfigProperties,gatewayKafkaConfig,grayLoadBalancer);
    }



}


