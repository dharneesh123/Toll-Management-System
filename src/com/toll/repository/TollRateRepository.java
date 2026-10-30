package com.toll.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.TollRate;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;

public final class TollRateRepository {
    private record RateKey(
            String plazaId, VehicleType vehicleType, TripType tripType, LocalDate effectiveDate) { }

    private final Map<RateKey, TollRate> rates = new LinkedHashMap<>();

    public void save(String plazaId, TollRate rate) {
        RateKey key = new RateKey(plazaId, rate.vehicleType(), rate.tripType(), rate.effectiveDate());
        if (rates.containsKey(key)) {
            throw new IllegalArgumentException("A rate already exists for that effective date");
        }
        rates.put(key, rate);
    }

    public TollRate findEffectiveRate(
            String plazaId, VehicleType vehicleType, TripType tripType, LocalDate date) {
        return rates.entrySet().stream()
                .filter(entry -> entry.getKey().plazaId().equals(plazaId)
                        && entry.getKey().vehicleType() == vehicleType
                        && entry.getKey().tripType() == tripType
                        && !entry.getKey().effectiveDate().isAfter(date))
                .map(Map.Entry::getValue)
                .max(Comparator.comparing(TollRate::effectiveDate))
                .orElse(null);
    }

    public boolean hasRatesFor(String plazaId, LocalDate date) {
        for (VehicleType vehicleType : VehicleType.values()) {
            for (TripType tripType : TripType.values()) {
                if (findEffectiveRate(plazaId, vehicleType, tripType, date) == null) return false;
            }
        }
        return true;
    }

    public List<TollRate> findAll() {
        return List.copyOf(rates.values());
    }
}
