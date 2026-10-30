package com.toll.models;

import java.util.Objects;
import com.toll.models.TollTypes.VehicleType;

public record Vehicle(
        String registration, VehicleType type, String ownerName, String phone, boolean emergency) {
    public Vehicle {
        requireText(registration, "registration");
        Objects.requireNonNull(type, "type");
        requireText(ownerName, "owner name");
        requireText(phone, "phone");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
