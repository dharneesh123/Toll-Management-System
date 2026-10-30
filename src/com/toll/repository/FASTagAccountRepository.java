package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.FASTagAccount;
import com.toll.models.TollTypes.AccountStatus;

public final class FASTagAccountRepository {
    private final Map<String, FASTagAccount> accounts = new LinkedHashMap<>();

    public void save(FASTagAccount account) {
        accounts.put(account.tagId(), account);
    }

    public FASTagAccount findById(String tagId) {
        return accounts.get(tagId);
    }

    public boolean existsById(String tagId) {
        return accounts.containsKey(tagId);
    }

    public FASTagAccount findNonClosedByVehicleRegistration(String registration) {
        return accounts.values().stream()
                .filter(account -> account.vehicleRegistration().equals(registration)
                        && account.status() != AccountStatus.CLOSED)
                .findFirst()
                .orElse(null);
    }

    public List<FASTagAccount> findAll() {
        return List.copyOf(accounts.values());
    }
}
