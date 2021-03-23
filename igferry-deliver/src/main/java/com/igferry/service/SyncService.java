package com.igferry.service;

import com.igferry.kafka.context.Response;
import com.igferry.pojo.qo.RequestQO;
import reactor.core.publisher.Mono;

public interface SyncService {

    public Mono<Response> syncRequest(RequestQO requestQO,String apiUrl,String method);

}
