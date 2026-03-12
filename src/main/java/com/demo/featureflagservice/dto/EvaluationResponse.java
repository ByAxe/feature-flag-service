package com.demo.featureflagservice.dto;

public record EvaluationResponse(String flagKey, boolean enabled, String reason) {
}
