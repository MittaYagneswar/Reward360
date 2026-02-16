package com.example.dashboard_backend.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CUSTOMERMS", fallback = CustomerTransactionFallback.class)
public interface CustomerTransactionClient {

    @GetMapping("/api/users/transactions/user/{userId}")
    List<String> getTransactionIdsByUserId(@PathVariable("userId") Long userId);
}


