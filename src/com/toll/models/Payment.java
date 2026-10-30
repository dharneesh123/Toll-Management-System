package com.toll.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.toll.models.TollTypes.PaymentMethod;
import com.toll.models.TollTypes.PaymentStatus;
import com.toll.models.TollTypes.PaymentType;

public record Payment(
        String id, PaymentType type, String referenceId, BigDecimal amount, LocalDateTime time,
        PaymentMethod method, PaymentStatus status, String laneOperator) {
    public Payment(PaymentType type, String referenceId, BigDecimal amount, LocalDateTime time,
                   PaymentMethod method, PaymentStatus status, String laneOperator) {
        this(UUID.randomUUID().toString(), type, referenceId, amount, time, method, status, laneOperator);
    }
}
