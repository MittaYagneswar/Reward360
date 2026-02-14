package com.example.dashboard_backend.client;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback implementation for CustomerTransactionClient
 * Returns empty data when CustomerMs service is unavailable
 */
@Component
public class CustomerTransactionFallback implements CustomerTransactionClient {

    @Override
    public List<String> getTransactionIdsByUserId(Long userId) {
        // Return empty list when CustomerMs is down
        System.err.println("CustomerMs service is unavailable. Returning empty transaction ID list for userId: " + userId);
        return Collections.emptyList();
    }
}