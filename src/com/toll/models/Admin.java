package com.toll.models;

import java.util.Objects;

public record Admin(String name) {
    public Admin {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Admin name is required");
        Objects.requireNonNull(name);
    }
}
