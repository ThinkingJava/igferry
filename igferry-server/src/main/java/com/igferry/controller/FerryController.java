package com.igferry.controller;

import com.igferry.kafka.context.Response;
import com.igferry.pojo.qo.RequestQO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: yangchenghuan
 * @Date: 2021/3/13 17:24
 * @Description: 测试类
 */
@Slf4j
@RestController
@RequestMapping("/service")
public class FerryController {

    @RequestMapping("/test")
    public @ResponseBody
    Response test(@RequestBody RequestQO requestQO) {
        log.info("==========request id", requestQO);
        return new Response<String>("ok",null, "0000", "success");
    }

}
