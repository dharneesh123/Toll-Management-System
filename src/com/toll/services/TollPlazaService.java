package com.toll.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import com.toll.models.Lane;
import com.toll.models.TollPlaza;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;
import com.toll.repository.AdminRepository;
import com.toll.repository.TollPlazaRepository;
import com.toll.repository.TollRateRepository;

public final class TollPlazaService {
    private final TollPlazaRepository tollPlazaRepository;
    private final TollRateRepository tollRateRepository;
    private final AdminRepository adminRepository;

    public TollPlazaService(
            TollPlazaRepository tollPlazaRepository,
            TollRateRepository tollRateRepository,
            AdminRepository adminRepository) {
        this.tollPlazaRepository = Objects.requireNonNull(tollPlazaRepository, "tollPlazaRepository");
        this.tollRateRepository = Objects.requireNonNull(tollRateRepository, "tollRateRepository");
        this.adminRepository = Objects.requireNonNull(adminRepository, "adminRepository");
    }

    public TollPlaza createPlaza(
            String adminName, String name, String highway, String location, List<Lane> lanes) {
        requireAdmin(adminName);
        if (lanes == null || lanes.size() < 2) {
            throw new IllegalArgumentException("A toll plaza must have at least two lanes");
        }

        TollPlaza plaza = new TollPlaza(name, highway, location);
        for (Lane lane : lanes) {
            plaza.addLane(Objects.requireNonNull(lane, "lanes must not contain null"));
        }

        tollPlazaRepository.save(plaza);
        return plaza;
    }

    public TollPlaza openPlaza(String adminName, String plazaId, LocalDate effectiveDate) {
        requireAdmin(adminName);
        if (plazaId == null || plazaId.isBlank()) {
            throw new IllegalArgumentException("Plaza ID is required");
        }
        Objects.requireNonNull(effectiveDate, "effectiveDate");

        TollPlaza plaza = tollPlazaRepository.findById(plazaId);
        if (plaza == null) {
            throw new IllegalArgumentException("Toll plaza not found: " + plazaId);
        }

        for (VehicleType vehicleType : VehicleType.values()) {
            if (tollRateRepository.findEffectiveRate(
                    plazaId, vehicleType, TripType.SINGLE, effectiveDate) == null) {
                throw new IllegalStateException(
                        "A single-trip toll rate is required for " + vehicleType
                                + " on " + effectiveDate);
            }
        }

        plaza.setOpen(true);
        tollPlazaRepository.save(plaza);
        return plaza;
    }

    private void requireAdmin(String adminName) {
        if (adminName == null || adminName.isBlank()
                || adminRepository.findByName(adminName) == null) {
            throw new SecurityException("A registered admin is required");
        }
    }
}
