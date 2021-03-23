package com.igferry.zookeeper.id;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/14
 *  @Description: workid
 */
public interface WorkerIdGenerator {

   public boolean init();

   public int getWorkId();

}
