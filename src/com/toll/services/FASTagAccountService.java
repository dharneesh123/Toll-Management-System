package com.toll.services;

import java.math.BigDecimal;
import java.util.Objects;
import com.toll.models.FASTagAccount;
import com.toll.models.TollTypes.AccountStatus;
import com.toll.repository.AdminRepository;
import com.toll.repository.FASTagAccountRepository;
import com.toll.repository.VehicleRepository;

public class FASTagAccountService {
    private static final BigDecimal MAX_BALANCE = new BigDecimal("50000");

    private final FASTagAccountRepository accountRepository;
    private final VehicleRepository vehicleRepository;
    private final AdminRepository adminRepository;

    public FASTagAccountService(
            FASTagAccountRepository accountRepository,
            VehicleRepository vehicleRepository,
            AdminRepository adminRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository");
        this.adminRepository = Objects.requireNonNull(adminRepository, "adminRepository");
    }

    public FASTagAccount createAccount(
            String adminName, String tagId, String vehicleRegistration, BigDecimal openingBalance) {
        requireAdmin(adminName);
        if (vehicleRegistration == null || vehicleRegistration.isBlank()
                || vehicleRepository.findByRegistration(vehicleRegistration) == null) {
            throw new IllegalArgumentException("A registered vehicle is required for a FASTag");
        }
        if (tagId == null || tagId.isBlank()) {
            throw new IllegalArgumentException("Tag ID is required");
        }
        Objects.requireNonNull(openingBalance, "openingBalance");
        if (openingBalance.signum() < 0 || openingBalance.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Opening balance must be between Rs. 0 and Rs. 50,000");
        }
        if (accountRepository.existsById(tagId)) {
            throw new IllegalArgumentException("FASTag already exists: " + tagId);
        }
        if (accountRepository.findNonClosedByVehicleRegistration(vehicleRegistration) != null) {
            throw new IllegalStateException("Vehicle already has a non-closed FASTag account");
        }

        FASTagAccount account = new FASTagAccount(tagId, vehicleRegistration, openingBalance);
        accountRepository.save(account);
        return account;
    }

    public FASTagAccount blacklistAccount(String adminName, String tagId) {
        requireAdmin(adminName);
        FASTagAccount account = findAccount(tagId);
        account.setBlacklisted(true);
        accountRepository.save(account);
        return account;
    }

    public FASTagAccount closeAccount(String adminName, String tagId) {
        requireAdmin(adminName);
        FASTagAccount account = findAccount(tagId);
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        return account;
    }

    public FASTagAccount findAccount(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            throw new IllegalArgumentException("Tag ID is required");
        }
        FASTagAccount account = accountRepository.findById(tagId);
        if (account == null) {
            throw new IllegalArgumentException("FASTag account not found: " + tagId);
        }
        return account;
    }

    private void requireAdmin(String adminName) {
        if (adminName == null || adminName.isBlank()
                || adminRepository.findByName(adminName) == null) {
            throw new SecurityException("A registered admin is required");
        }
    }
}
