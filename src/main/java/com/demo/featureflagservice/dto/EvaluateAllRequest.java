package com.demo.featureflagservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluateAllRequest(@NotBlank @Size(max = 255) String userId) {
}
