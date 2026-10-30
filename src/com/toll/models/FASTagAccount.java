package com.toll.models;

import java.math.BigDecimal;
import java.util.Objects;
import com.toll.models.TollTypes.AccountStatus;

public final class FASTagAccount {
    private final String tagId;
    private final String vehicleRegistration;
    private BigDecimal balance;
    private AccountStatus status;
    private boolean blacklisted;

    public FASTagAccount(String tagId, String vehicleRegistration, BigDecimal balance) {
        if (tagId == null || tagId.isBlank()) throw new IllegalArgumentException("Tag ID is required");
        if (vehicleRegistration == null || vehicleRegistration.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration is required");
        }
        Objects.requireNonNull(balance, "balance");
        if (balance.signum() < 0) throw new IllegalArgumentException("Balance cannot be negative");
        this.tagId = tagId;
        this.vehicleRegistration = vehicleRegistration;
        this.balance = balance;
        this.status = balance.compareTo(new BigDecimal("100")) < 0
                ? AccountStatus.BLOCKED : AccountStatus.ACTIVE;
    }

    public String tagId() { return tagId; }
    public String vehicleRegistration() { return vehicleRegistration; }
    public BigDecimal balance() { return balance; }
    public AccountStatus status() { return status; }
    public boolean blacklisted() { return blacklisted; }

    public void setStatus(AccountStatus status) {
        Objects.requireNonNull(status, "status");
        if (status == AccountStatus.ACTIVE && balance.compareTo(new BigDecimal("100")) < 0) {
            throw new IllegalStateException("A tag with balance below Rs. 100 cannot be Active");
        }
        this.status = status;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public void credit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) throw new IllegalArgumentException("Credit cannot be negative");
        if (balance.add(amount).compareTo(new BigDecimal("50000")) > 0) {
            throw new IllegalArgumentException("Balance cannot exceed Rs. 50,000");
        }
        balance = balance.add(amount);
        if (status == AccountStatus.BLOCKED && balance.compareTo(new BigDecimal("100")) >= 0) {
            status = AccountStatus.ACTIVE;
        }
    }

    public void debit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) throw new IllegalArgumentException("Debit cannot be negative");
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance = balance.subtract(amount);
        if (balance.compareTo(new BigDecimal("100")) < 0) status = AccountStatus.BLOCKED;
    }
}
