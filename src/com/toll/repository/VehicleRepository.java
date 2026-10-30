package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.Vehicle;

public final class VehicleRepository {
    private final Map<String, Vehicle> vehicles = new LinkedHashMap<>();

    public Vehicle saveIfAbsent(Vehicle vehicle) {
        return vehicles.putIfAbsent(vehicle.registration(), vehicle);
    }

    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.registration(), vehicle);
    }

    public Vehicle findByRegistration(String registration) {
        return vehicles.get(registration);
    }

    public List<Vehicle> findAll() {
        return List.copyOf(vehicles.values());
    }
}
