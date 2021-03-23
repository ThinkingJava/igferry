package com.igferry.spi;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description:
 */
public interface LoadBalance {
    /**
     * Select an instance based on the load balancing algorithm
     * @param instances
     * @return
     */
    ServiceInstance chooseOne(List<ServiceInstance> instances);
}
