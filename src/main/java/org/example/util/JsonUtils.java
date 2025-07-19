package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.List;

public class JsonUtils {
    private static final Logger log = LogManager.getLogger(JsonUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * obj to json string
     */
    public static String toJson(Object obj) {
        try {

            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Json serialize error", e);
            return null;
        }
    }

    /**
     * json string to obj
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Json deserialize error", e);
            return null;
        }
    }

    /**
     * json to object list
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            log.error("Json deserialize to list error", e);
            return null;
        }
    }

    public static <T> T fromPathToObj(String path, Class<T> clazz) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            return objectMapper.readValue(is, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON from " + path, e);
        }
    }

    public static <T> List<T> fromPathToObjList(String path, Class<T> clazz) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(is, listType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON from " + path, e);
        }
    }
}