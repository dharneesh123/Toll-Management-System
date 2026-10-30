package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.Dispute;

public final class DisputeRepository {
    private final Map<String, Dispute> disputes = new LinkedHashMap<>();

    public void save(Dispute dispute) {
        disputes.put(dispute.id(), dispute);
    }

    public Dispute findById(String id) {
        return disputes.get(id);
    }

    public Dispute findByTransactionId(String transactionId) {
        return disputes.values().stream()
                .filter(dispute -> dispute.transactionId().equals(transactionId))
                .findFirst()
                .orElse(null);
    }

    public List<Dispute> findAll() {
        return List.copyOf(disputes.values());
    }
}
