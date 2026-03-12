package com.demo.featureflagservice.repository;

public record GlobalStatsRow(long totalEvaluations, long uniqueUsers) {
}
