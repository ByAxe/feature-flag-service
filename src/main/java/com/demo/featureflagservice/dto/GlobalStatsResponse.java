package com.demo.featureflagservice.dto;

public record GlobalStatsResponse(long totalEvaluations, long uniqueUsers, long activeFlagsCount) {
}
