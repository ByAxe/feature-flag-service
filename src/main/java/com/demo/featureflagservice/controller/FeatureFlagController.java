package com.demo.featureflagservice.controller;

import com.demo.featureflagservice.dto.EvaluateAllRequest;
import com.demo.featureflagservice.dto.EvaluateRequest;
import com.demo.featureflagservice.dto.EvaluationResponse;
import com.demo.featureflagservice.dto.FeatureFlagRequest;
import com.demo.featureflagservice.dto.FeatureFlagResponse;
import com.demo.featureflagservice.dto.FeatureFlagUpdateRequest;
import com.demo.featureflagservice.service.FeatureFlagService;
import com.demo.featureflagservice.service.evaluation.EvaluationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flags")
@Validated
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final EvaluationService evaluationService;

    public FeatureFlagController(FeatureFlagService featureFlagService, EvaluationService evaluationService) {
        this.featureFlagService = featureFlagService;
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public ResponseEntity<FeatureFlagResponse> create(@Valid @RequestBody FeatureFlagRequest request) {
        FeatureFlagResponse response = featureFlagService.create(request);
        return ResponseEntity.created(URI.create("/api/flags/" + response.key())).body(response);
    }

    @GetMapping
    public List<FeatureFlagResponse> list() {
        return featureFlagService.list();
    }

    @GetMapping("/{key}")
    public FeatureFlagResponse getByKey(@PathVariable String key) {
        return featureFlagService.getResponseByKey(key);
    }

    @PutMapping("/{key}")
    public FeatureFlagResponse update(@PathVariable String key, @Valid @RequestBody FeatureFlagUpdateRequest request) {
        return featureFlagService.update(key, request);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        featureFlagService.delete(key);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evaluate")
    public EvaluationResponse evaluate(
            @RequestHeader(name = "X-Request-Id", required = false) String requestId,
            @RequestHeader(name = "Request-Id", required = false) String legacyRequestId,
            @Valid @RequestBody EvaluateRequest request) {
        String resolvedRequestId = resolveRequestId(requestId, legacyRequestId);
        var result = evaluationService.evaluate(request.flagKey(), request.userId(), resolvedRequestId);
        return new EvaluationResponse(result.flagKey(), result.enabled(), result.reason().name());
    }

    @PostMapping("/evaluate-all")
    public List<EvaluationResponse> evaluateAll(
            @RequestHeader(name = "X-Request-Id", required = false) String requestId,
            @RequestHeader(name = "Request-Id", required = false) String legacyRequestId,
            @Valid @RequestBody EvaluateAllRequest request) {
        String resolvedRequestId = resolveRequestId(requestId, legacyRequestId);
        return evaluationService.evaluateAll(request.userId(), resolvedRequestId).stream()
                .map(result -> new EvaluationResponse(result.flagKey(), result.enabled(), result.reason().name()))
                .toList();
    }

    private String resolveRequestId(String requestId, String legacyRequestId) {
        return java.util.stream.Stream.of(requestId, legacyRequestId)
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }
}
