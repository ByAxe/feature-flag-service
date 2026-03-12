package com.demo.featureflagservice.service.evaluation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class HashingRolloutStrategy {

    public boolean isEnabled(String flagKey, String userId, int rolloutPercentage) {
        if (rolloutPercentage <= 0) {
            return false;
        }
        if (rolloutPercentage >= 100) {
            return true;
        }
        int bucket = Math.floorMod(hash(flagKey + ":" + userId), 100);
        return bucket < rolloutPercentage;
    }

    private int hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            int result = 0;
            for (int i = 0; i < 4; i++) {
                result = (result << 8) | (bytes[i] & 0xff);
            }
            return result;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
