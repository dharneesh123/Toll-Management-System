package com.toll.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import com.toll.models.TollRate;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;
import com.toll.repository.AdminRepository;
import com.toll.repository.TollPlazaRepository;
import com.toll.repository.TollRateRepository;

public class TollRateService {
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final TollRateRepository tollRateRepository;
    private final TollPlazaRepository tollPlazaRepository;
    private final AdminRepository adminRepository;

    public TollRateService(
            TollRateRepository tollRateRepository,
            TollPlazaRepository tollPlazaRepository,
            AdminRepository adminRepository) {
        this.tollRateRepository = Objects.requireNonNull(tollRateRepository, "tollRateRepository");
        this.tollPlazaRepository = Objects.requireNonNull(tollPlazaRepository, "tollPlazaRepository");
        this.adminRepository = Objects.requireNonNull(adminRepository, "adminRepository");
    }

    public TollRate setRate(
            String adminName,
            String plazaId,
            VehicleType vehicleType,
            TripType tripType,
            LocalDate effectiveDate,
            BigDecimal amount) {
        requireAdmin(adminName);
        requirePlaza(plazaId);
        TollRate rate = new TollRate(vehicleType, tripType, effectiveDate, amount);
        tollRateRepository.save(plazaId, rate);
        return rate;
    }

    public TollRate findEffectiveRate(
            String plazaId, VehicleType vehicleType, TripType tripType, LocalDate date) {
        requirePlaza(plazaId);
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(tripType, "tripType");
        Objects.requireNonNull(date, "date");
        TollRate rate = tollRateRepository.findEffectiveRate(plazaId, vehicleType, tripType, date);
        if (rate == null) {
            throw new IllegalStateException(
                    "No effective " + tripType + " rate for " + vehicleType + " at plaza " + plazaId);
        }
        return rate;
    }

    public BigDecimal calculateTollAmount(
            String plazaId, VehicleType vehicleType, TripType tripType, LocalDate date) {
        TollRate singleRate = findEffectiveRate(plazaId, vehicleType, TripType.SINGLE, date);
        BigDecimal amount = tripType == TripType.RETURN
                ? singleRate.amount().multiply(HALF)
                : singleRate.amount();
        return amount.divide(FIVE, 0, RoundingMode.HALF_UP).multiply(FIVE);
    }

    private void requirePlaza(String plazaId) {
        if (plazaId == null || plazaId.isBlank()) {
            throw new IllegalArgumentException("Plaza ID is required");
        }
        if (tollPlazaRepository.findById(plazaId) == null) {
            throw new IllegalArgumentException("Toll plaza not found: " + plazaId);
        }
    }

    private void requireAdmin(String adminName) {
        if (adminName == null || adminName.isBlank()
                || adminRepository.findByName(adminName) == null) {
            throw new SecurityException("A registered admin is required");
        }
    }
}
