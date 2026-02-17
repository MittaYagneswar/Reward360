package com.example.dashboard_backend.controller;

import com.example.dashboard_backend.client.CustomerTransactionClient;
import com.example.dashboard_backend.dto.TransactionDTO;
import com.example.dashboard_backend.entity.Transaction;
import com.example.dashboard_backend.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionRepository txRepo;
    private final CustomerTransactionClient customerTransactionClient;

    @Autowired
    public TransactionController(TransactionRepository txRepo,
                                CustomerTransactionClient customerTransactionClient) {
        this.txRepo = txRepo;
        this.customerTransactionClient = customerTransactionClient;
    }

    // ===== List with filters (Points-Based) =====
    @GetMapping
    public List<Transaction> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String riskLevel,   // LOW/MEDIUM/HIGH/CRITICAL
            @RequestParam(required = false) String status,      // CLEARED/REVIEW/BLOCKED
            // removed type filter
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer minPoints,
            @RequestParam(required = false) Integer maxPoints,
            @RequestParam(required = false) String store        // store name filter
    ) {
        String risk = riskLevel != null ? riskLevel.toUpperCase() : null;
        String st = status != null ? status.toUpperCase() : null;
    // removed type filter logic

    if (allNull(accountId, risk, st, from, to, minPoints, maxPoints, store)) {
            List<Transaction> transactions = txRepo.findTop100ByOrderByCreatedAtDesc();
            enrichWithCustomerData(transactions);
            return transactions;
        }
        
        // Filter points transactions
        List<Transaction> transactions = txRepo.findAll().stream()
            .filter(tx -> accountId == null || accountId.equals(tx.getAccountId()))
            .filter(tx -> risk == null || risk.equals(tx.getRiskLevel()))
            .filter(tx -> st == null || st.equals(tx.getStatus()))
            // removed type filter
            .filter(tx -> from == null || tx.getCreatedAt().isAfter(from))
            .filter(tx -> to == null || tx.getCreatedAt().isBefore(to))
            .filter(tx -> minPoints == null || (tx.getPointsEarned() + tx.getPointsRedeemed()) >= minPoints)
            .filter(tx -> maxPoints == null || (tx.getPointsEarned() + tx.getPointsRedeemed()) <= maxPoints)
            .filter(tx -> store == null || (tx.getStore() != null && tx.getStore().toLowerCase().contains(store.toLowerCase())))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(100)
            .toList();
        
        enrichWithCustomerData(transactions);
        return transactions;
    }

    private void enrichWithCustomerData(List<Transaction> transactions) {
        // No enrichment needed, as note/type are not part of Fraud_MS Transaction entity
    }

    private boolean allNull(Object... vals) {
        for (Object v : vals) if (v != null) return false;
        return true;
    }

    // ===== Get one =====
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> get(@PathVariable Long id) {
        Optional<Transaction> opt = txRepo.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ===== Create (Points Transaction) =====
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Transaction tx) {
        try {
            if (tx.getStatus() == null || tx.getStatus().isBlank()) tx.setStatus("CLEARED");
            else tx.setStatus(tx.getStatus().toUpperCase());

            if (tx.getRiskLevel() != null) tx.setRiskLevel(tx.getRiskLevel().toUpperCase());
            if (tx.getCreatedAt() == null) tx.setCreatedAt(Instant.now());
            tx.setUpdatedAt(null);

            Transaction savedTx = txRepo.save(tx);
            return ResponseEntity.ok(savedTx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating transaction: " + e.getMessage());
        }
    }

    // ===== Quick actions =====
    @PostMapping("/{id}/review")
    public ResponseEntity<Transaction> markReview(@PathVariable Long id) {
        return updateStatus(id, "REVIEW");
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Transaction> markBlocked(@PathVariable Long id) {
        return updateStatus(id, "BLOCKED");
    }

    @PostMapping("/{id}/clear")
    public ResponseEntity<Transaction> markCleared(@PathVariable Long id) {
        return updateStatus(id, "CLEARED");
    }

    private ResponseEntity<Transaction> updateStatus(Long id, String newStatus) {
        Optional<Transaction> opt = txRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Transaction tx = opt.get();
        tx.setStatus(newStatus.toUpperCase());
        tx.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(txRepo.save(tx));
    }

    // ===== Export CSV (applies the same filters) =====
    @GetMapping(value = "/export", produces = "text/csv")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            // removed type filter
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer minPoints,
            @RequestParam(required = false) Integer maxPoints,
            @RequestParam(required = false) String store
    ) throws Exception {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");
    response.setContentType("text/csv");
    // Do NOT set any CORS headers here. Global CORS config will handle it.

    List<Transaction> data = list(accountId, riskLevel, status, from, to, minPoints, maxPoints, store);

        try (PrintWriter writer = response.getWriter()) {
            writer.println("id,externalId,pointsEarned,pointsRedeemed,store,date,expiry,accountId,riskLevel,status,createdAt,updatedAt");
            for (Transaction t : data) {
        writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
            nullSafe(t.getId()),
            csv(t.getExternalId()),
            csv(t.getPointsEarned()),
            csv(t.getPointsRedeemed()),
            csv(t.getStore()),
            csv(t.getDate()),
            csv(t.getExpiry()),
            csv(t.getAccountId()),
            csv(t.getRiskLevel()),
            csv(t.getStatus()),
            csv(t.getCreatedAt()),
            csv(t.getUpdatedAt())
        );
            }
        }
    }

    private Object nullSafe(Object o) { return o == null ? "" : o; }

    private String csv(Object o) {
        if (o == null) return "";
        String s = o.toString();
        // Escape quotes and wrap when needed
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }
    
    /**
     * Get transaction data (id, type, note) from CustomerMs for a specific user
     * This endpoint uses Feign client to fetch transactions from CustomerMS
     */
    @GetMapping("/customer/{userId}/transactions")
    public ResponseEntity<List<TransactionDTO>> getCustomerTransactionsByUserId(@PathVariable Long userId) {
        List<TransactionDTO> transactions = customerTransactionClient.getTransactionsByUserId(userId);
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get all transaction data (id, type, note) from CustomerMs
     * This endpoint uses Feign client to fetch all transactions from CustomerMS
     */
    @GetMapping("/customer/transactions/all")
    public ResponseEntity<List<TransactionDTO>> getAllCustomerTransactions() {
        List<TransactionDTO> transactions = customerTransactionClient.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }
}
