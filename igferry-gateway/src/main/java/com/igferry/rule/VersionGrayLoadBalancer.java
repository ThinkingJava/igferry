package com.igferry.rule;

import cn.hutool.core.collection.CollUtil;
import com.igferry.cache.LoadBalanceFactory;
import com.igferry.config.ServerConfigProperties;
import com.igferry.spi.LoadBalance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.support.NotFoundException;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class VersionGrayLoadBalancer implements GrayLoadBalancer {

    private ServerConfigProperties serverConfigProperties;

    private DiscoveryClient discoveryClient;

    /**
     * 根据serviceId 筛选可用服务
     * @param serviceId 服务ID
     * @return
     */
    @Override
    public ServiceInstance choose(String serviceId) {
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
        // 注册中心无实例 抛出异常
        if (CollUtil.isEmpty(serviceInstances)) {
            log.warn("No instance available for {}", serviceId);
            throw new NotFoundException("No instance available for " + serviceId);
        }
         //Select an instance based on the load balancing algorithm
        LoadBalance loadBalance = LoadBalanceFactory.getInstance(serverConfigProperties.getLoadBalance(), serviceId);
        ServiceInstance serviceInstance = loadBalance.chooseOne(serviceInstances);
        return serviceInstance;
    }

}

