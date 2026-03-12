package com.demo.featureflagservice.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluateAllRequest(@NotBlank String userId) {
}
