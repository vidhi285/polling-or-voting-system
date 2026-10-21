package com.pollingsystem.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollingsystem.exception.BadRequestException;

/** The only place that touches Jackson. JSON is never built by string concatenation. */
public final class Json {
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public byte[] write(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise response", e);
        }
    }

    public <T> T read(byte[] body, Class<T> type) {
        if (body == null || body.length == 0) {
            throw new BadRequestException("EMPTY_BODY", "Request body is required.");
        }
        try {
            T value = mapper.readValue(body, type);
            if (value == null) {
                throw new BadRequestException("MALFORMED_JSON", "Request body must be a JSON object.");
            }
            return value;
        } catch (IOException e) { // includes JsonProcessingException (malformed or wrongly-typed JSON)
            throw new BadRequestException("MALFORMED_JSON", "Request body is not valid JSON for this endpoint.");
        }
    }
}
