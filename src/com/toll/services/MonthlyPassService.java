package com.toll.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import com.toll.models.MonthlyPass;
import com.toll.repository.MonthlyPassRepository;
import com.toll.repository.TollPlazaRepository;
import com.toll.repository.VehicleRepository;

public class MonthlyPassService {
    private final MonthlyPassRepository monthlyPassRepository;
    private final TollPlazaRepository tollPlazaRepository;
    private final VehicleRepository vehicleRepository;

    public MonthlyPassService(
            MonthlyPassRepository monthlyPassRepository,
            TollPlazaRepository tollPlazaRepository,
            VehicleRepository vehicleRepository) {
        this.monthlyPassRepository = Objects.requireNonNull(monthlyPassRepository, "monthlyPassRepository");
        this.tollPlazaRepository = Objects.requireNonNull(tollPlazaRepository, "tollPlazaRepository");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository");
    }

    public MonthlyPass purchasePass(
            String vehicleRegistration,
            String plazaId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal fee,
            LocalDate purchaseDate) {
        if (vehicleRegistration == null || vehicleRegistration.isBlank()
                || vehicleRepository.findByRegistration(vehicleRegistration) == null) {
            throw new IllegalArgumentException("A registered vehicle is required for a monthly pass");
        }
        if (plazaId == null || plazaId.isBlank() || tollPlazaRepository.findById(plazaId) == null) {
            throw new IllegalArgumentException("A valid toll plaza is required for a monthly pass");
        }
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(fee, "fee");
        Objects.requireNonNull(purchaseDate, "purchaseDate");
        if (fee.signum() < 0) {
            throw new IllegalArgumentException("Pass fee cannot be negative");
        }

        boolean overlapsExistingPass = monthlyPassRepository.findAll().stream()
                .anyMatch(pass -> pass.vehicleRegistration().equals(vehicleRegistration)
                        && pass.plazaId().equals(plazaId)
                        && !startDate.isAfter(pass.endDate())
                        && !endDate.isBefore(pass.startDate()));
        if (overlapsExistingPass) {
            throw new IllegalStateException(
                    "Vehicle already has a pass overlapping the requested validity period");
        }

        MonthlyPass pass = new MonthlyPass(
                vehicleRegistration, plazaId, startDate, endDate, fee, purchaseDate);
        monthlyPassRepository.save(pass);
        return pass;
    }

    public MonthlyPass findValidPass(String vehicleRegistration, String plazaId, LocalDate date) {
        if (vehicleRegistration == null || vehicleRegistration.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration is required");
        }
        if (plazaId == null || plazaId.isBlank()) {
            throw new IllegalArgumentException("Plaza ID is required");
        }
        Objects.requireNonNull(date, "date");
        return monthlyPassRepository.findAll().stream()
                .filter(pass -> pass.vehicleRegistration().equals(vehicleRegistration)
                        && pass.plazaId().equals(plazaId)
                        && !date.isBefore(pass.startDate())
                        && !date.isAfter(pass.endDate()))
                .findFirst()
                .orElse(null);
    }
}
