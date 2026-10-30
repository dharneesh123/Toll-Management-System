package com.toll.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record MonthlyPass(
        String id, String vehicleRegistration, String plazaId, LocalDate startDate,
        LocalDate endDate, BigDecimal fee, LocalDate purchaseDate) {
    public MonthlyPass(String vehicleRegistration, String plazaId, LocalDate startDate,
                       LocalDate endDate, BigDecimal fee, LocalDate purchaseDate) {
        this(UUID.randomUUID().toString(), vehicleRegistration, plazaId,
                startDate, endDate, fee, purchaseDate);
    }

    public MonthlyPass {
        if (vehicleRegistration == null || vehicleRegistration.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration is required");
        }
        if (plazaId == null || plazaId.isBlank()) throw new IllegalArgumentException("Plaza ID is required");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(fee, "fee");
        Objects.requireNonNull(purchaseDate, "purchaseDate");
        if (fee.signum() < 0) throw new IllegalArgumentException("Pass fee cannot be negative");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("Pass end date precedes start date");
    }
}
