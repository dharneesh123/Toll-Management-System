package com.toll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.toll.models.Admin;
import com.toll.models.Lane;
import com.toll.models.TollTypes.LaneStatus;
import com.toll.models.TollTypes.LaneType;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;
import com.toll.models.Vehicle;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        TollManagementSystem system = new TollManagementSystem();
        Admin admin = new Admin("admin");
        LocalDate today = LocalDate.now();
        String plazaId = system.createPlaza(admin, "Central Toll Plaza", "NH 44", "Salem",
                List.of(
                        new Lane(1, LaneType.FASTAG, LaneStatus.OPEN),
                        new Lane(2, LaneType.CASH, LaneStatus.OPEN),
                        new Lane(3, LaneType.EMERGENCY, LaneStatus.OPEN)));

        for (VehicleType type : VehicleType.values()) {
            BigDecimal singleRate = new BigDecimal("100");
            system.setRate(admin, plazaId, type, TripType.SINGLE, today, singleRate);
            system.setRate(admin, plazaId, type, TripType.RETURN, today,
                    singleRate.divide(new BigDecimal("2")));
        }
        system.openPlaza(admin, plazaId, today);
        system.registerVehicle(new Vehicle("TN01AB1234", VehicleType.CAR, "A. Kumar", "9000000000", false));
        system.createTagAccount(admin, "FASTAG-1001", "TN01AB1234", new BigDecimal("1000"));

        var result = system.processFastagCrossing(
                plazaId, system.laneId(plazaId, 1), "FASTAG-1001", "TN01AB1234",
                VehicleType.CAR, system.laneId(plazaId, 2), "operator-1", LocalDateTime.now());
        System.out.println("Barrier opened: " + result.barrierOpened());
        System.out.println("Recorded transactions: " + result.transactions().size());
    }
}
