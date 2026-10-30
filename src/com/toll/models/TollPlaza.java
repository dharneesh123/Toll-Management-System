package com.toll.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;

public final class TollPlaza {
    private final String id = UUID.randomUUID().toString();
    private final String name;
    private final String highway;
    private final String location;
    private final Map<String, Lane> lanes = new HashMap<>();
    private final Map<Integer, String> laneIdsByNumber = new HashMap<>();
    private final List<TollRate> rates = new ArrayList<>();
    private boolean open;

    public TollPlaza(String name, String highway, String location) {
        this.name = requiredText(name, "plaza name");
        this.highway = requiredText(highway, "highway name");
        this.location = requiredText(location, "location");
    }

    public String id() { return id; }
    public String name() { return name; }
    public String highway() { return highway; }
    public String location() { return location; }
    public boolean isOpen() { return open; }

    public void addLane(Lane lane) {
        if (laneIdsByNumber.containsKey(lane.number())) {
            throw new IllegalArgumentException("Lane numbers must be unique within a plaza");
        }
        lanes.put(lane.id(), lane);
        laneIdsByNumber.put(lane.number(), lane.id());
    }

    public Lane lane(String id) { return lanes.get(id); }
    public Lane laneByNumber(int number) {
        String laneId = laneIdsByNumber.get(number);
        return laneId == null ? null : lanes.get(laneId);
    }
    public boolean containsLane(String id) { return lanes.containsKey(id); }

    public void addRate(TollRate rate) {
        boolean duplicate = rates.stream().anyMatch(existing ->
                existing.vehicleType() == rate.vehicleType()
                        && existing.tripType() == rate.tripType()
                        && existing.effectiveDate().equals(rate.effectiveDate()));
        if (duplicate) throw new IllegalArgumentException("A rate already exists for that effective date");
        rates.add(rate);
    }

    public TollRate rateAt(VehicleType type, TripType trip, LocalDate date) {
        return rates.stream()
                .filter(rate -> rate.vehicleType() == type && rate.tripType() == trip
                        && !rate.effectiveDate().isAfter(date))
                .max(Comparator.comparing(TollRate::effectiveDate))
                .orElse(null);
    }

    public boolean hasRatesFor(LocalDate date) {
        for (VehicleType type : VehicleType.values()) {
            for (TripType trip : TripType.values()) {
                if (rateAt(type, trip, date) == null) return false;
            }
        }
        return true;
    }

    public void setOpen(boolean open) { this.open = open; }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
