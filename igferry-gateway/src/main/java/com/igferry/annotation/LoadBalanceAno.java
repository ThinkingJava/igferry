package com.igferry.annotation;

import java.lang.annotation.*;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13
 *  @Description: 负载均衡注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoadBalanceAno {

    String value() default "";
}
