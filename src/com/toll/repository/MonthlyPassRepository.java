package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.MonthlyPass;

public final class MonthlyPassRepository {
    private final Map<String, MonthlyPass> passes = new LinkedHashMap<>();

    public void save(MonthlyPass pass) {
        passes.put(pass.id(), pass);
    }

    public MonthlyPass findById(String id) {
        return passes.get(id);
    }

    public List<MonthlyPass> findAll() {
        return List.copyOf(passes.values());
    }
}
