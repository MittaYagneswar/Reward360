package com.example.dashboard_backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "CUSTOMERMS", fallback = CustomerTransactionFallback.class)
public interface CustomerTransactionClient {

    @GetMapping("/api/users/{userId}/transaction-ids")
    List<String> getTransactionIdsByUserId(@PathVariable("userId") Long userId);
}


