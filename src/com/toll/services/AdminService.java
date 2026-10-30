package com.toll.services;

import java.util.Objects;
import com.toll.models.Admin;
import com.toll.repository.AdminRepository;

public class AdminService {
    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = Objects.requireNonNull(adminRepository, "adminRepository");
    }

    public Admin registerAdmin(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Admin name is required");
        }
        if (adminRepository.findByName(name) != null) {
            throw new IllegalArgumentException("Admin is already registered: " + name);
        }

        Admin admin = new Admin(name);
        adminRepository.save(admin);
        return admin;
    }

    public Admin findAdmin(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Admin name is required");
        }
        Admin admin = adminRepository.findByName(name);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found: " + name);
        }
        return admin;
    }
}
