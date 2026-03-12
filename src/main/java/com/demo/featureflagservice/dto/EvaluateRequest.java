package com.demo.featureflagservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluateRequest(
        @NotBlank String flagKey,
        @NotBlank @Size(max = 255) String userId) {
}
