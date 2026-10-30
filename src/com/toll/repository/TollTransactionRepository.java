package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.TollTransaction;

public final class TollTransactionRepository {
    private final Map<String, TollTransaction> transactions = new LinkedHashMap<>();

    public void save(TollTransaction transaction) {
        transactions.put(transaction.id(), transaction);
    }

    public TollTransaction findById(String id) {
        return transactions.get(id);
    }

    public List<TollTransaction> findAll() {
        return List.copyOf(transactions.values());
    }
}
