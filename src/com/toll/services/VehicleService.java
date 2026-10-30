package com.toll.services;

import java.util.List;
import java.util.Objects;
import com.toll.models.Vehicle;
import com.toll.repository.VehicleRepository;

public final class VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository");
    }

    public Vehicle registerVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        Vehicle existing = vehicleRepository.saveIfAbsent(vehicle);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "A vehicle is already registered with registration: " + vehicle.registration());
        }
        return vehicle;
    }

    public Vehicle findByRegistration(String registration) {
        if (registration == null || registration.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration is required");
        }

        Vehicle vehicle = vehicleRepository.findByRegistration(registration);
        if (vehicle == null) {
            throw new IllegalArgumentException(
                    "Vehicle not found with registration: " + registration);
        }
        return vehicle;
    }

    public List<Vehicle> findAllVehicles() {
        return vehicleRepository.findAll();
    }
}
