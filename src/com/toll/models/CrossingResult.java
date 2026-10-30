package com.toll.models;

import java.util.List;

public record CrossingResult(List<TollTransaction> transactions, boolean barrierOpened, boolean sentToCash) {
    public CrossingResult {
        transactions = List.copyOf(transactions);
    }
}
