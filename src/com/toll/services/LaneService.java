package com.toll.services;

import java.util.Objects;
import com.toll.models.Lane;
import com.toll.models.TollPlaza;
import com.toll.models.TollTypes.LaneStatus;
import com.toll.models.TollTypes.LaneType;
import com.toll.repository.AdminRepository;
import com.toll.repository.TollPlazaRepository;

public final class LaneService {
    private final TollPlazaRepository tollPlazaRepository;
    private final AdminRepository adminRepository;

    public LaneService(TollPlazaRepository tollPlazaRepository, AdminRepository adminRepository) {
        this.tollPlazaRepository = Objects.requireNonNull(tollPlazaRepository, "tollPlazaRepository");
        this.adminRepository = Objects.requireNonNull(adminRepository, "adminRepository");
    }

    public Lane createLane(
            String adminName,
            String plazaId,
            int laneNumber,
            LaneType laneType,
            LaneStatus laneStatus) {
        requireAdmin(adminName);
        TollPlaza plaza = findPlaza(plazaId);

        Lane lane = new Lane(laneNumber, laneType, laneStatus);
        plaza.addLane(lane);
        tollPlazaRepository.save(plaza);
        return lane;
    }

    public Lane updateLaneStatus(
            String adminName, String plazaId, String laneId, LaneStatus laneStatus) {
        requireAdmin(adminName);
        TollPlaza plaza = findPlaza(plazaId);
        if (laneId == null || laneId.isBlank()) {
            throw new IllegalArgumentException("Lane ID is required");
        }
        Objects.requireNonNull(laneStatus, "laneStatus");

        Lane lane = plaza.lane(laneId);
        if (lane == null) {
            throw new IllegalArgumentException("Lane not found in plaza: " + laneId);
        }

        lane.setStatus(laneStatus);
        tollPlazaRepository.save(plaza);
        return lane;
    }

    private TollPlaza findPlaza(String plazaId) {
        if (plazaId == null || plazaId.isBlank()) {
            throw new IllegalArgumentException("Plaza ID is required");
        }

        TollPlaza plaza = tollPlazaRepository.findById(plazaId);
        if (plaza == null) {
            throw new IllegalArgumentException("Toll plaza not found: " + plazaId);
        }
        return plaza;
    }

    private void requireAdmin(String adminName) {
        if (adminName == null || adminName.isBlank()
                || adminRepository.findByName(adminName) == null) {
            throw new SecurityException("A registered admin is required");
        }
    }
}
