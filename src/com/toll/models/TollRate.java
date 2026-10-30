package com.toll.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;

public record TollRate(
        VehicleType vehicleType, TripType tripType, LocalDate effectiveDate, BigDecimal amount) {
    public TollRate {
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(tripType, "tripType");
        Objects.requireNonNull(effectiveDate, "effectiveDate");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new IllegalArgumentException("Rate must be greater than zero");
    }
}
