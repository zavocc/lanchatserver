package com.compprogroup.lan.chatapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class for JSON serialization and deserialization
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // Configure ObjectMapper for LocalDateTime support
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Serialize an object to JSON string
     */
    public static String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Deserialize JSON string to Message object
     */
    public static Message fromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, Message.class);
    }
    
    /**
     * Deserialize JSON string to specific class
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }
    
    /**
     * Check if a string is valid JSON
     */
    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}