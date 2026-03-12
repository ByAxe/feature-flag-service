package com.demo.featureflagservice.controller;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import java.util.UUID;
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
        String key = "new-checkout-flow-" + UUID.randomUUID();
        String createPayload = """
                {
                  "key": "%s",
                  "description": "Enable checkout flow",
                  "enabled": true,
                  "rolloutPercentage": 25,
                  "targetUserIds": ["qa-1", "qa-2"]
                }
                """.formatted(key);

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(key));

        mockMvc.perform(get("/api/flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].key", hasItem(key)));

        mockMvc.perform(get("/api/flags/" + key))
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

        mockMvc.perform(put("/api/flags/" + key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(delete("/api/flags/" + key))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/flags/" + key))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateCreatePayloadBeforePersisting() throws Exception {
        String invalidPayload = """
                {
                  "key": "",
                  "description": "invalid",
                  "enabled": true,
                  "rolloutPercentage": 150,
                  "targetUserIds": ["qa-user"]
                }
                """;

        mockMvc.perform(post("/api/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error", not(emptyOrNullString())));
    }

    @Test
    void shouldReturnNotFoundForMissingCrudResources() throws Exception {
        mockMvc.perform(get("/api/flags/missing-flag"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error", not(emptyOrNullString())));

        mockMvc.perform(put("/api/flags/missing-flag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Attempt update missing",
                                  "enabled": true,
                                  "rolloutPercentage": 50,
                                  "targetUserIds": []
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error", not(emptyOrNullString())));

        mockMvc.perform(delete("/api/flags/missing-flag"))
                .andExpect(status().isNotFound())
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
    void shouldRejectDuplicateKeysAndKeepStoredKey() throws Exception {
        String createPayload = """
                {
                  "key": "canonical-flag",
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
                                  "key": "canonical-flag",
                                  "description": "Should fail with conflict",
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
