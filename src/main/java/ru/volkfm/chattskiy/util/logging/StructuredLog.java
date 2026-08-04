package ru.volkfm.chattskiy.util.logging;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Helper class to mark objects to be logged as objects (e.g. json objects) instead of plain strings
 */
public class StructuredLog {
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SESSION_ID_KEY = "sessionId";
    public static final String USER_ID_KEY = "userId";
    public static final String OBJECT_KEY = "object";
    public static final String DEST_KEY = "dest";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    // Springs StructuredLogFormatter serializes Map to json object
    public static Map<String, Object> object(Object o) {
        return MAPPER.convertValue(o, new TypeReference<>() {});
    }
}
