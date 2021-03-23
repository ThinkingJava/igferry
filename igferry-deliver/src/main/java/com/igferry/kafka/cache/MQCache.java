package com.igferry.kafka.cache;

import reactor.core.publisher.MonoSink;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 *  @author: yangchenghuan
 *  @Date: 2021/3/13 20:41
 *  @Description: 缓存请求
 */
public class MQCache {

    public static final ConcurrentSkipListMap<String, MonoSink<Object>> KEY_MONO_SINK;

    public static void addMonoSink(final String key, final MonoSink<Object> monoSink) {
        MQCache.KEY_MONO_SINK.put(key, monoSink);
    }

    public static MonoSink<Object> getMonoSink(final String key) {
        return MQCache.KEY_MONO_SINK.get(key);
    }

    public static void removeMonoSink(final String key) {
        MQCache.KEY_MONO_SINK.remove(key);
    }

    static {
        KEY_MONO_SINK = new ConcurrentSkipListMap<String, MonoSink<Object>>();
    }

}
