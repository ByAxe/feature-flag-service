package com.demo.featureflagservice.service.evaluation;

public record EvaluationResult(String flagKey, boolean enabled, EvaluationReason reason) {
}
