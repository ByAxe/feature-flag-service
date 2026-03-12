package com.demo.featureflagservice.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluateRequest(
        @NotBlank String flagKey,
        @NotBlank String userId) {
}
