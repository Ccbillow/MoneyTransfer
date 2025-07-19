package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.util.List;

public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 对象转 JSON 字符串 */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
//            log.error("Json serialize error", e);
            return null;
        }
    }

    /** JSON 字符串转对象 */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
//            log.error("Json deserialize error", e);
            return null;
        }
    }

    /** JSON 字符串转 List<T> */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
//            log.error("Json deserialize to list error", e);
            return null;
        }
    }

    /** JSON 字符串格式化输出 */
    public static String toPrettyJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
//            log.error("Json pretty serialize error", e);
            return null;
        }
    }
}