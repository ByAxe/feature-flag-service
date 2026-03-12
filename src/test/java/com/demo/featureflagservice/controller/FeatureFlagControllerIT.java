package com.demo.featureflagservice.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeatureFlagControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateReadUpdateAndDeleteFlag() throws Exception {
        String createPayload = """
                {
                  "key": "new-checkout-flow",
                  "description": "Enable checkout flow",
                  "enabled": true,
                  "rolloutPercentage": 25,
                  "targetUserIds": ["qa-1", "qa-2"]
                }
                """;

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("new-checkout-flow"));

        mockMvc.perform(get("/api/flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/flags/new-checkout-flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        String updatePayload = """
                {
                  "description": "Updated description",
                  "enabled": false,
                  "rolloutPercentage": 0,
                  "targetUserIds": ["qa-1"]
                }
                """;

        mockMvc.perform(put("/api/flags/new-checkout-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(delete("/api/flags/new-checkout-flow"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/flags/new-checkout-flow"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDuplicateKey() throws Exception {
        String payload = """
                {
                  "key": "duplicate-flag",
                  "description": "First",
                  "enabled": true,
                  "rolloutPercentage": 100,
                  "targetUserIds": []
                }
                """;

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldCanonicalizeCaseWhenCreatingAndUpdatingFlags() throws Exception {
        String createPayload = """
                {
                  "key": "Canonical-Flag",
                  "description": "Canonicalized duplicate demo",
                  "enabled": true,
                  "rolloutPercentage": 10,
                  "targetUserIds": ["qa-user-1"]
                }
                """;

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("canonical-flag"));

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "CANONICAL-FLAG",
                                  "description": "Should fail",
                                  "enabled": false,
                                  "rolloutPercentage": 0,
                                  "targetUserIds": []
                                }
                                """))
                .andExpect(status().isConflict());

        String updatePayload = """
                {
                  "description": "updated canonical key should stay immutable",
                  "enabled": false,
                  "rolloutPercentage": 100,
                  "targetUserIds": ["qa-user-1", "qa-user-2"]
                }
                """;

        mockMvc.perform(put("/api/flags/CANONICAL-FLAG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("canonical-flag"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.rolloutPercentage").value(100));

        mockMvc.perform(delete("/api/flags/canonical-flag"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldIgnoreKeyInUpdatePayloadAndKeepStoredKey() throws Exception {
        String createPayload = """
                {
                  "key": "immutable-update-key",
                  "description": "Original",
                  "enabled": true,
                  "rolloutPercentage": 30,
                  "targetUserIds": []
                }
                """;

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("immutable-update-key"));

        String updatePayload = """
                {
                  "key": "mutable-attempt",
                  "description": "Attempted rename",
                  "enabled": true,
                  "rolloutPercentage": 40,
                  "targetUserIds": []
                }
                """;

        mockMvc.perform(put("/api/flags/immutable-update-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("immutable-update-key"))
                .andExpect(jsonPath("$.description").value("Attempted rename"));
    }
}
