package com.igferry.rule;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.server.reactive.ServerHttpRequest;

public interface GrayLoadBalancer {

    /**
     * 根据serviceId 筛选可用服务
     * @param serviceId 服务ID
     * @return
     */
    ServiceInstance choose(String serviceId);

}
