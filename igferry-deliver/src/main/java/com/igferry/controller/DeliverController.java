package com.igferry.controller;

import com.igferry.kafka.context.Response;
import com.igferry.pojo.qo.RequestQO;
import com.igferry.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: 测试类
 */
@Slf4j
@RestController
@RequestMapping("/service")
public class DeliverController {

    @Autowired
    private SyncService syncService;

    @RequestMapping("/synctest")
    public Mono<Response> syncTest(@RequestBody RequestQO requestQO) {
        log.info("==========request test");
        String api = "/igferry-server/service/test";
        String method = "POST";
        Mono<Response> responseMono =  syncService.syncRequest(requestQO,api,method);
        return responseMono;
    }

}
