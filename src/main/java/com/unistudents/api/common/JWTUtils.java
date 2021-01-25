package com.unistudents.api.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

import static java.util.Base64.getUrlDecoder;

public class JWTUtils {

    public static String[] decoded(String JWTEncoded) {
        try {
            Base64.Decoder decoder = getUrlDecoder();
            String[] split = JWTEncoded.split("\\.");

            JsonNode node = new ObjectMapper().readTree(decoder.decode(split[1]));
            String department = node.get("roles").get(0).get("authorizations").get(0).get("department").asText();
            String category = node.get("roles").get(0).get("authorizations").get(0).get("category").asText();

            return new String[]{department, category};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
