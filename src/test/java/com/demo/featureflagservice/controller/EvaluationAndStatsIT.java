package com.demo.featureflagservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import com.demo.featureflagservice.repository.EvaluationLogRepository;
import com.demo.featureflagservice.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class EvaluationAndStatsIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Test
    void shouldEvaluateByTargetListAndReturnStatsAndLogRequestCorrelation(CapturedOutput output) throws Exception {
        featureFlagRepository.deleteAll();
        evaluationLogRepository.deleteAll();

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "beta-dashboard",
                                  "description": "Beta dashboard",
                                  "enabled": true,
                                  "rolloutPercentage": 10,
                                  "targetUserIds": ["qa-user"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/flags/evaluate")
                        .header("X-Request-Id", "eval-it-id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flagKey": "beta-dashboard",
                                  "userId": "qa-user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagKey").value("beta-dashboard"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.reason").value("TARGET_LIST"));

        mockMvc.perform(post("/api/flags/evaluate-all")
                        .header("X-Request-Id", "eval-it-id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "qa-user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].flagKey").value(hasItem("beta-dashboard")));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvaluations").value(2))
                .andExpect(jsonPath("$.uniqueUsers").value(1))
                .andExpect(jsonPath("$.activeFlagsCount").value(1));

        mockMvc.perform(get("/api/stats/beta-dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvaluations").value(2))
                .andExpect(jsonPath("$.trueCount").value(2));

        assertThat(output.getOut()).contains("\"requestId\":\"eval-it-id-1\"");
        assertThat(output.getOut()).contains("\"flagKey\":\"beta-dashboard\"");
        assertThat(output.getOut()).contains("\"userId\":\"qa-user\"");
        assertThat(output.getOut()).contains("\"result\":true");
        assertThat(output.getOut()).contains("\"reason\":\"TARGET_LIST\"");
        assertThat(output.getOut()).contains("\"latencyMs\":");
    }

    @Test
    void shouldEvaluateDeterministicallyForRepeatedRequests() throws Exception {
        String flagKey = "deterministic-rollout-" + UUID.randomUUID();
        String createPayload = String.format("""
                {
                  "key": "%s",
                  "description": "deterministic rollout",
                  "enabled": true,
                  "rolloutPercentage": 42,
                  "targetUserIds": []
                }
                """, flagKey);
        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        String evaluatePayload = String.format("""
                {
                  "flagKey": "%s",
                  "userId": "user-a"
                }
                """, flagKey);

        mockMvc.perform(post("/api/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("PERCENTAGE_ROLLOUT"));

        mockMvc.perform(post("/api/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("PERCENTAGE_ROLLOUT"));
    }

    @Test
    void shouldReturnNotFoundForUnknownFlag() throws Exception {
        mockMvc.perform(post("/api/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flagKey": "missing-flag",
                                  "userId": "qa-user"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnNotFoundForUnknownFlagStats() throws Exception {
        mockMvc.perform(get("/api/stats/unknown-flag"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldValidateBadRequests() throws Exception {
        mockMvc.perform(post("/api/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flagKey": "",
                                  "userId": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/flags/evaluate-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldValidateMalformedJsonAndMissingFields() throws Exception {
        mockMvc.perform(post("/api/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", not(emptyOrNullString())));

        mockMvc.perform(post("/api/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flagKey\":\"beta-dashboard\", \"userId\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", not(emptyOrNullString())));

        mockMvc.perform(post("/api/flags/evaluate-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foo\":\"bar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
