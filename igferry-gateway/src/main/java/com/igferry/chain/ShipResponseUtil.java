package com.igferry.chain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description:
 */ 
public class ShipResponseUtil {

    private static Gson gson = new GsonBuilder().create();

    /**
     * @param exchange
     * @param resp
     */
    public static Mono<Void> doResponse(ServerWebExchange exchange, String resp) {
        Assert.notNull(resp, "response object can't be null");
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(resp.getBytes())));
    }

}
