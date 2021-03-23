package com.igferry.spi.balance;

import com.igferry.annotation.LoadBalanceAno;
import com.igferry.constants.LoadBalanceConstants;
import com.igferry.spi.LoadBalance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: 轮询算法
 */
@LoadBalanceAno(LoadBalanceConstants.ROUND)
public class FullRoundBalance implements LoadBalance {

    private volatile int index;

    @Override
    public synchronized ServiceInstance chooseOne(List<ServiceInstance> instances) {
        if (index == instances.size()) {
            index = 0;
        }
        return instances.get(index++);
    }
}
