package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.TollPlaza;

public final class TollPlazaRepository {
    private final Map<String, TollPlaza> plazas = new LinkedHashMap<>();

    public void save(TollPlaza plaza) {
        plazas.put(plaza.id(), plaza);
    }

    public TollPlaza findById(String id) {
        return plazas.get(id);
    }

    public List<TollPlaza> findAll() {
        return List.copyOf(plazas.values());
    }
}
