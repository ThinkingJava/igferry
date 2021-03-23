package com.igferry.config;

import com.igferry.constants.LoadBalanceConstants;
import com.igferry.constants.IgferryExceptionEnum;
import com.igferry.exception.IGFerryException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13 16:28
 *  @Description: 网关配置类
 */
@Component
@ConfigurationProperties(prefix = "igferry.gate")
public class ServerConfigProperties implements InitializingBean {
    /**
     * 负载均衡算法，默认轮询
     */
    private String loadBalance = LoadBalanceConstants.ROUND;
    /**
     * 网关超时时间，默认3s
     */
    private Long timeOutMillis = 3000L;
    /**
     * 缓存刷新间隔，默认10s
     */
    private Long cacheRefreshInterval = 10L;

    private Integer webSocketPort;

    public Integer getWebSocketPort() {
        return webSocketPort;
    }

    public void setWebSocketPort(Integer webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    public Long getCacheRefreshInterval() {
        return cacheRefreshInterval;
    }

    public void setCacheRefreshInterval(Long cacheRefreshInterval) {
        this.cacheRefreshInterval = cacheRefreshInterval;
    }

    public Long getTimeOutMillis() {
        return timeOutMillis;
    }

    public void setTimeOutMillis(Long timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

    public String getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.webSocketPort == null || this.webSocketPort <= 0) {
            throw new IGFerryException(IgferryExceptionEnum.CONFIG_ERROR);
        }
    }

}
