package com.toll.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.toll.models.TollTypes.TransactionStatus;
import com.toll.models.TollTypes.TripType;

public record TollTransaction(
        String id,
        String vehicleRegistration,
        String plazaId,
        String laneId,
        LocalDateTime time,
        BigDecimal amount,
        TransactionStatus status,
        TripType tripType,
        boolean flaggedForReview,
        String tagId,
        String details) { }
