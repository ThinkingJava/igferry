package com.igferry.zookeeper;

import com.igferry.config.ServerConfig;
import com.igferry.config.ZookeeperConfig;
import com.igferry.zookeeper.id.WorkerIdGenerator;
import com.igferry.zookeeper.id.impl.ZooKeeperWorkerIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/14
 *  @Description: zookeeper注入bean
 */
@Slf4j
@Configuration
@Lazy
public class ZookeeperConfiguration {

    @Value("${server.port}")
    private String port;

    @Bean
    public WorkerIdGenerator getWorkerIdGenerator(ZookeeperConfig zookeeperConfig, ServerConfig serverConfig){
        WorkerIdGenerator workerIdGenerator = new ZooKeeperWorkerIdGenerator(serverConfig.getAddress(),port,zookeeperConfig.getZkServer(),zookeeperConfig);
        workerIdGenerator.init();
        return workerIdGenerator;
    }

}
