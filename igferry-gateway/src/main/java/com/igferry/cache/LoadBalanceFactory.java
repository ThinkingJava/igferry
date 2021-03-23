package com.igferry.cache;

import com.igferry.annotation.LoadBalanceAno;
import com.igferry.exception.IGFerryException;
import com.igferry.spi.LoadBalance;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13 
 *  @Description: 均衡负载工程类
 */
public final class LoadBalanceFactory {

    /**
     * key: appName:version
     */
    private static final Map<String, LoadBalance> LOAD_BALANCE_MAP = new ConcurrentHashMap<>();

    private LoadBalanceFactory(){

    }

    /**
     * get LoadBalance instance
     * @param name
     * @param appName
     * @return
     */
    public static LoadBalance getInstance(final String name, String appName) {
        String key = appName ;
        return LOAD_BALANCE_MAP.computeIfAbsent(key, (k) -> getLoadBalance(name));
    }

    /**
     * use spi to match load balance algorithm by server config
     *
     * @param name
     * @return
     */
    private static LoadBalance getLoadBalance(String name) {
        ServiceLoader<LoadBalance> loader = ServiceLoader.load(LoadBalance.class);
        Iterator<LoadBalance> iterator = loader.iterator();
        while (iterator.hasNext()) {
            LoadBalance loadBalance = iterator.next();
            LoadBalanceAno ano = loadBalance.getClass().getAnnotation(LoadBalanceAno.class);
            Assert.notNull(ano, "load balance name can not be empty!");
            if (name.equals(ano.value())) {
                return loadBalance;
            }
        }
        throw new IGFerryException("invalid load balance config");
    }
}
