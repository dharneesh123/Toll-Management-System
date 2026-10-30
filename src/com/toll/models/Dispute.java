package com.toll.models;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Dispute {
    private final String id = UUID.randomUUID().toString();
    private final String transactionId;
    private final String reason;
    private final LocalDateTime submittedAt;
    private boolean resolved;
    private boolean refunded;

    public Dispute(String transactionId, String reason, LocalDateTime submittedAt) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Dispute reason is required");
        if (submittedAt == null) throw new IllegalArgumentException("Submission time is required");
        this.transactionId = transactionId;
        this.reason = reason;
        this.submittedAt = submittedAt;
    }

    public String id() { return id; }
    public String transactionId() { return transactionId; }
    public String reason() { return reason; }
    public LocalDateTime submittedAt() { return submittedAt; }
    public boolean resolved() { return resolved; }
    public boolean refunded() { return refunded; }
    public void resolve(boolean refunded) {
        if (resolved) throw new IllegalStateException("Dispute has already been resolved");
        this.refunded = refunded;
        this.resolved = true;
    }
}
