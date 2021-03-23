package com.igferry.spi.balance;

import com.igferry.annotation.LoadBalanceAno;
import com.igferry.constants.LoadBalanceConstants;
import com.igferry.spi.LoadBalance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Random;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: 随机算法
 */
@LoadBalanceAno(LoadBalanceConstants.RANDOM)
public class RandomBalance implements LoadBalance {

    private static Random random = new Random();

    @Override
    public ServiceInstance chooseOne(List<ServiceInstance> instances) {
        return instances.get(random.nextInt(instances.size()));
    }
}
