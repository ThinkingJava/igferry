package com.igferry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacksonUtil {

    private static final String SOURCE_FORMAT = "\"%s\":\"(.*?)\"";
    private static final String TARGET_FORMAT = "\"%s\":\"%s\"";
    private static final ObjectMapper objectMapper;
    private static final ToStringSerializer longToStringSerializer;

    static {
        objectMapper = new ObjectMapper();
        longToStringSerializer = new ToStringSerializer();
        JacksonUtil.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer((Class)Long.class, (JsonSerializer)JacksonUtil.longToStringSerializer);
        simpleModule.addSerializer((Class)Long.TYPE, (JsonSerializer)JacksonUtil.longToStringSerializer);
//        simpleModule.addAbstractTypeMapping((Class)IPage.class, (Class)PageQueryResultDefaultImpl.class);
        JacksonUtil.objectMapper.registerModule((Module)simpleModule);
    }

    public static String toJson(final Object object) {
        try {
            return JacksonUtil.objectMapper.writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("\u5e8f\u5217\u5316\u4e3aJSON\u5931\u8d25: " + e.getMessage(), (Throwable)e);
        }
    }

    public static <T> T fromJson(final InputStream inputStream, final Class<T> klass) {
        String json;
        try {
            json = CharStreams.toString((Readable)new InputStreamReader(inputStream, Charsets.UTF_8));
        }
        catch (IOException e) {
            throw new IllegalStateException("\u8bfb\u53d6\u8f93\u5165\u6d41\u5931\u8d25: " + e.getMessage(), e);
        }
        final JavaType javaType = JacksonUtil.objectMapper.getTypeFactory().constructType((Type)klass);
        return fromJsonViaJavaType(json, javaType);
    }
    public static <T> T fromJson(final String json, final Class<T> klass) {
        final JavaType javaType = JacksonUtil.objectMapper.getTypeFactory().constructType(klass);
        return fromJsonViaJavaType(json, javaType);
    }

    public static <T> T fromJson(final String json, final Type type) {
        final JavaType javaType = JacksonUtil.objectMapper.getTypeFactory().constructType(type);
        return fromJsonViaJavaType(json, javaType);
    }

    public static <T> T fromJson(final String json, final Class<T> rootClass, final Class<?>... subParametricClasses) {
        final JavaType javaType = getNestedParametricType(rootClass, subParametricClasses);
        return fromJsonViaJavaType(json, javaType);
    }

    private static <T> T fromJsonViaJavaType(final String json, final JavaType javaType) {
        try {
            return (T)JacksonUtil.objectMapper.readerFor(javaType).readValue(json);
        }
        catch (IOException e) {
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u5931\u8d25:" + e.getMessage(), e);
        }
    }

    public static JavaType constructType(final Type type) {
        return JacksonUtil.objectMapper.getTypeFactory().constructType(type);
    }

    public static Map<String, Object> jsonToMap(final String json) {
        return jsonToMap(json, String.class, Object.class);
    }

    public static <K, V> Map<K, V> jsonToMap(final String json, final Class<K> kClass, final Class<V> vClass) {
        try {
            final JavaType kType = JacksonUtil.objectMapper.getTypeFactory().constructType((Type)kClass);
            final JavaType vType = JacksonUtil.objectMapper.getTypeFactory().constructType((Type)vClass);
            final MapType mapType = JacksonUtil.objectMapper.getTypeFactory().constructMapType((Class) HashMap.class, kType, vType);
            return (Map<K, V>)JacksonUtil.objectMapper.readValue(json, (JavaType)mapType);
        }
        catch (IOException e) {
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u5931\u8d25: " + e.getMessage(), e);
        }
    }

    public static <T> List<T> fromJsonArray(final String json, final Class<T> clazz) {
        final CollectionType javaType = JacksonUtil.objectMapper.getTypeFactory().constructCollectionType((Class) List.class, (Class)clazz);
        return fromJsonViaJavaType(json, (JavaType)javaType);
    }

    public static <K, V> List<Map<K, V>> fromJsonArray(final String json, final Class<K> kClass, final Class<V> vClass) {
        final JavaType kType = JacksonUtil.objectMapper.getTypeFactory().constructType((Type)kClass);
        final JavaType vType = JacksonUtil.objectMapper.getTypeFactory().constructType((Type)vClass);
        final MapType mapType = JacksonUtil.objectMapper.getTypeFactory().constructMapType((Class)HashMap.class, kType, vType);
        final CollectionType javaType = JacksonUtil.objectMapper.getTypeFactory().constructCollectionType((Class)List.class, (JavaType)mapType);
        return fromJsonViaJavaType(json, (JavaType)javaType);
    }

    public static ObjectReader readerFor(final JavaType javaType) {
        return JacksonUtil.objectMapper.readerFor(javaType);
    }

    public static ObjectReader readerFor(final Class<?> type) {
        return JacksonUtil.objectMapper.readerFor((Class)type);
    }

    public static <T> T mapToJavaBean(final Map map, final Class<T> klass) {
        return (T)JacksonUtil.objectMapper.convertValue((Object)map, (Class)klass);
    }

    public static Map<String, Object> beanToMap(final Object o) {
        if (o == null) {
            return null;
        }
        return (Map<String, Object>)JacksonUtil.objectMapper.convertValue(o, (Class)Map.class);
    }

    public static JavaType getNestedParametricType(final Class<?> rootClass, final Class<?>... subParameterClasses) {
        if (subParameterClasses == null || subParameterClasses.length == 0) {
            return JacksonUtil.objectMapper.getTypeFactory().constructType((Type)rootClass);
        }
        if (subParameterClasses.length == 1) {
            return JacksonUtil.objectMapper.getTypeFactory().constructParametricType((Class)rootClass, new Class[] { subParameterClasses[0] });
        }
        JavaType nestedType = null;
        for (int i = subParameterClasses.length - 2; i >= 0; --i) {
            if (nestedType == null) {
                nestedType = JacksonUtil.objectMapper.getTypeFactory().constructParametricType((Class)subParameterClasses[i], new Class[] { subParameterClasses[i + 1] });
            }
            else {
                nestedType = JacksonUtil.objectMapper.getTypeFactory().constructParametricType((Class)subParameterClasses[i], new JavaType[] { nestedType });
            }
        }
        return JacksonUtil.objectMapper.getTypeFactory().constructParametricType((Class)rootClass, new JavaType[] { nestedType });
    }

    public static JavaType getParametricType(final Class<?> paramtricClass, final Class<?>... elementClass) {
        return JacksonUtil.objectMapper.getTypeFactory().constructParametricType((Class)paramtricClass, (Class[])elementClass);
    }

    public static String toPrettyJson(final Object object) {
        try {
            return JacksonUtil.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("\u5e8f\u5217\u5316\u4e3aJSON\u5931\u8d25: " + e.getMessage(), (Throwable)e);
        }
    }

    public static Object getFirst(final String json) {
        final Map map = jsonToMap(json);
        if (map != null && !map.isEmpty()) {
            return map.values().stream().findFirst().get();
        }
        return null;
    }

    public static Object get(final String json, final String key) {
        final Map<String, Object> map = jsonToMap(json);
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    public static String replaceFieldValue(final String json, final String fieldName, final String newValue) {
        return json.replaceAll(String.format("\"%s\":\"(.*?)\"", fieldName), String.format("\"%s\":\"%s\"", fieldName, newValue));
    }

    public static <T> T fromJsonClass(final String json, final Class<T> rootClass) throws JsonProcessingException {
        // 将Java对象序列化为Json字符串
//      String jsonString =  JacksonUtil.objectMapper.writeValueAsString(json);
//        // 将Json字符串反序列化为Java对象
//       return objectMapper.readValue(jsonString, rootClass);
        ObjectMapper objectMapper = new ObjectMapper();
        // 将Java对象序列化为Json字符串
        String asString = objectMapper.writeValueAsString(json);
        // 将Json字符串反序列化为Java对象
        T employee = objectMapper.readValue(asString, rootClass);
        return employee;
    }

}
