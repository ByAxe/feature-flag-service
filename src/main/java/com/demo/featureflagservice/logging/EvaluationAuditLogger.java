package com.demo.featureflagservice.logging;

import com.demo.featureflagservice.service.evaluation.EvaluationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EvaluationAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("evaluation-audit");

    private final ObjectMapper objectMapper;

    public EvaluationAuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logSuccess(String requestId, String userId, EvaluationResult result, long latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flagKey", result.flagKey());
        payload.put("userId", userId);
        payload.put("result", result.enabled());
        payload.put("reason", result.reason().name());
        payload.put("latencyMs", latencyMs);
        payload.put("requestId", requestId);
        LOGGER.info(toJson(payload));
    }

    public void logFailure(String requestId, String flagKey, String userId, String error, long latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flagKey", flagKey);
        payload.put("userId", userId);
        payload.put("result", null);
        payload.put("reason", null);
        payload.put("error", error);
        payload.put("latencyMs", latencyMs);
        payload.put("requestId", requestId);
        LOGGER.warn(toJson(payload));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return payload.toString();
        }
    }
}
