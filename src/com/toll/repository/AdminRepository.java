package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.Admin;

public final class AdminRepository {
    private final Map<String, Admin> admins = new LinkedHashMap<>();

    public void save(Admin admin) {
        admins.put(admin.name(), admin);
    }

    public Admin findByName(String name) {
        return admins.get(name);
    }

    public List<Admin> findAll() {
        return List.copyOf(admins.values());
    }
}
