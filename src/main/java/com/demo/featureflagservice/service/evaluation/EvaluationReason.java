package com.demo.featureflagservice.service.evaluation;

public enum EvaluationReason {
    DISABLED,
    TARGET_LIST,
    FULL_ROLLOUT,
    NO_ROLLOUT,
    PERCENTAGE_ROLLOUT
}
