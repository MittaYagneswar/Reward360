package com.example.dashboard_backend.service.impl;

import com.example.dashboard_backend.entity.Transaction;
import com.example.dashboard_backend.repository.TransactionRepository;
import com.example.dashboard_backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService {
    
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Override
    public List<Transaction> list(String accountId, String riskLevel, String status, String paymentMethod,
                                   Instant from, Instant to, String q) {
        return transactionRepository.findFiltered(accountId, riskLevel, status, paymentMethod, from, to, q);
    }
    
    @Override
    public Optional<Transaction> get(Long id) {
        return transactionRepository.findById(id);
    }
    
    @Override
    @Transactional
    public Transaction create(Transaction tx) {
        if (tx.getCreatedAt() == null) tx.setCreatedAt(Instant.now());
        Transaction saved = transactionRepository.save(tx);
        applyFraudRules(saved);
        return transactionRepository.save(saved);
    }
    
    @Override
    @Transactional
    public Optional<Transaction> updateStatus(Long id, String newStatus) {
        Optional<Transaction> txOpt = transactionRepository.findById(id);
        if (txOpt.isPresent()) {
            Transaction tx = txOpt.get();
            tx.setStatus(newStatus);
            return Optional.of(transactionRepository.save(tx));
        }
        return Optional.empty();
    }
    
    @Override
    @Transactional
    public Transaction markForReview(Long transactionId, String userId, String username, String reason) {
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        tx.setStatus("REVIEW");
        tx.setRiskLevel("HIGH");
        return transactionRepository.save(tx);
    }
    
    @Override
    @Transactional
    public Transaction blockTransaction(Long transactionId, String userId, String username, String reason) {
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        tx.setStatus("BLOCKED");
        tx.setRiskLevel("CRITICAL");
        return transactionRepository.save(tx);
    }
    
    @Override
    @Transactional
    public Transaction clearTransaction(Long transactionId, String userId, String username, String reason) {
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        tx.setStatus("APPROVED");
        tx.setRiskLevel("LOW");
        return transactionRepository.save(tx);
    }
    
    @Override
    @Transactional
    public void bulkMarkForReview(Long[] transactionIds, String userId, String username, String reason) {
        for (Long id : transactionIds) {
            markForReview(id, userId, username, reason);
        }
    }
    
    @Override
    @Transactional
    public void bulkBlockTransactions(Long[] transactionIds, String userId, String username, String reason) {
        for (Long id : transactionIds) {
            blockTransaction(id, userId, username, reason);
        }
    }
    
    @Override
    @Transactional
    public void processTransaction(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        // Apply fraud detection rules
        applyFraudRules(tx);
        
        transactionRepository.save(tx);
    }
    
    /**
     * Apply fraud detection rules to a transaction
     * Only flags TRULY SUSPICIOUS patterns, not normal user behavior
     */
    private void applyFraudRules(Transaction tx) {
        // IMPORTANT: Don't flag normal CLAIM transactions - only check REDEMPTIONS
        if (tx.getType() != null && tx.getType().equals("CLAIM")) {
            // Claims are normal - just award points without fraud checking
            tx.setRiskLevel("LOW");
            tx.setStatus("CLEARED");
            return;
        }
        
        // Rule 1: EXTREMELY high value redemptions (potential account takeover)
        // Changed from 10,000 to 50,000 to avoid false positives
        if (tx.getPointsRedeemed() != null && tx.getPointsRedeemed() > 50000) {
            tx.setRiskLevel("HIGH");
            tx.setStatus("REVIEW");
            tx.setDescription("Flagged: Extremely high value redemption (>50,000 points)");
            return;
        }
        
        // Rule 2: RAPID multiple redemptions (bot-like behavior)
        // Changed from 5 redemptions in 10 minutes to 10 redemptions in 5 minutes
        if (tx.getType() != null && tx.getType().equals("REDEMPTION")) {
            if (tx.getUserId() != null) {
                Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
                List<Transaction> recentRedemptions = transactionRepository
                    .findByUserAndTypeAndCreatedAtAfter(tx.getUserId(), "REDEMPTION", fiveMinutesAgo);
                
                if (recentRedemptions.size() >= 10) {
                    tx.setRiskLevel("HIGH");
                    tx.setStatus("REVIEW");
                    tx.setDescription("Flagged: Rapid multiple redemptions (>10 in 5 min) - Bot suspected");
                    return;
                }
            }
        }
        
        // Rule 3: EXTREME account activity (account compromise suspected)
        // Changed from 20 transactions in 1 hour to 50 transactions in 1 hour
        if (tx.getAccountId() != null) {
            Instant oneHourAgo = Instant.now().minusSeconds(3600);
            List<Transaction> recentTransactions = transactionRepository
                .findByAccountIdAndCreatedAtAfter(tx.getAccountId(), oneHourAgo);
            
            if (recentTransactions.size() > 50) {
                tx.setRiskLevel("MEDIUM");
                tx.setStatus("REVIEW");
                tx.setDescription("Flagged: Extreme account activity (>50 transactions in 1 hour)");
                return;
            }
        }
        
        // Rule 4: Multiple HIGH value redemptions in short time (suspicious pattern)
        if (tx.getType() != null && tx.getType().equals("REDEMPTION") && tx.getPointsRedeemed() != null) {
            if (tx.getPointsRedeemed() > 20000 && tx.getUserId() != null) {
                Instant thirtyMinutesAgo = Instant.now().minusSeconds(1800);
                List<Transaction> recentHighValueRedemptions = transactionRepository
                    .findByUserAndTypeAndCreatedAtAfter(tx.getUserId(), "REDEMPTION", thirtyMinutesAgo);
                
                // Count how many were high value
                long highValueCount = recentHighValueRedemptions.stream()
                    .filter(t -> t.getPointsRedeemed() != null && t.getPointsRedeemed() > 20000)
                    .count();
                
                if (highValueCount >= 3) {
                    tx.setRiskLevel("HIGH");
                    tx.setStatus("REVIEW");
                    tx.setDescription("Flagged: Multiple high-value redemptions (>20k points) in 30 minutes");
                    return;
                }
            }
        }
        
        // Default: No fraud detected - transaction is legitimate
        if (tx.getRiskLevel() == null) {
            tx.setRiskLevel("LOW");
        }
        if (tx.getStatus() == null || tx.getStatus().isEmpty()) {
            tx.setStatus("CLEARED");
        }
    }
    
    @Override
    @Transactional
    public void reprocessAllTransactions() {
        List<Transaction> allTransactions = transactionRepository.findAll();
        for (Transaction tx : allTransactions) {
            applyFraudRules(tx);
            transactionRepository.save(tx);
        }
    }
}
