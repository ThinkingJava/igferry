package com.igferry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

@Data
@Component
@ConfigurationProperties(prefix = "igferry.zookeeper")
public class ZookeeperConfig {

    //zookeeper地址
    private String zkServer;
    //zookeeper前缀路径
    private String prefixZkPath;

    //本地缓存workid地址
    String propPath = System.getProperty("java.io.tmpdir") + File.separator + "ferry/{port}/workerID.properties";

    //保存所有数据持久的节点
    String pathForever =  "/ferry";

}
