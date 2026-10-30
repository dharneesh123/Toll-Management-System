package com.toll;

import static com.toll.models.TollTypes.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.toll.models.Admin;
import com.toll.models.CrossingResult;
import com.toll.models.Lane;
import com.toll.models.TollTransaction;
import com.toll.models.Vehicle;

public final class TollManagementSystemTest {
    private static final Admin ADMIN = new Admin("test-admin");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 30);
    private static final LocalDateTime TIME = DATE.atTime(10, 0);

    private TollManagementSystemTest() { }

    public static void main(String[] args) {
        TollManagementSystem system = new TollManagementSystem();
        String plaza = system.createPlaza(ADMIN, "Test Plaza", "NH 1", "Test Location",
                List.of(new Lane(1, LaneType.FASTAG, LaneStatus.OPEN),
                        new Lane(2, LaneType.CASH, LaneStatus.OPEN),
                        new Lane(3, LaneType.EMERGENCY, LaneStatus.OPEN)));
        for (VehicleType type : VehicleType.values()) {
            system.setRate(ADMIN, plaza, type, TripType.SINGLE, DATE, money("100"));
            system.setRate(ADMIN, plaza, type, TripType.RETURN, DATE, money("1"));
        }
        system.openPlaza(ADMIN, plaza, DATE);
        String fastagLane = system.laneId(plaza, 1);
        String cashLane = system.laneId(plaza, 2);

        system.registerVehicle(new Vehicle("CAR-1", VehicleType.CAR, "Owner One", "9000000001", false));
        system.createTagAccount(ADMIN, "TAG-1", "CAR-1", money("250"));
        CrossingResult first = system.processFastagCrossing(plaza, fastagLane, "TAG-1",
                "CAR-1", VehicleType.CAR, cashLane, "operator", TIME);
        check(first.barrierOpened() && first.transactions().get(0).amount().equals(money("100")),
                "successful FASTag crossing");
        check(system.tagBalance("TAG-1").equals(money("150"))
                && system.lowBalanceAlerts().size() == 1, "debit and low-balance alert");

        CrossingResult duplicate = system.processFastagCrossing(plaza, fastagLane, "TAG-1",
                "CAR-1", VehicleType.CAR, cashLane, "operator", TIME.plusMinutes(1));
        check(!duplicate.barrierOpened(), "duplicate tag crossing is refused");
        TollTransaction returning = system.processFastagCrossing(plaza, fastagLane, "TAG-1",
                "CAR-1", VehicleType.CAR, cashLane, "operator", TIME.plusMinutes(3))
                .transactions().get(0);
        check(returning.tripType() == TripType.RETURN && returning.amount().equals(money("50")),
                "return rate is half the single rate");

        system.registerVehicle(new Vehicle("CAR-2", VehicleType.CAR, "Owner Two", "9000000002", false));
        CrossingResult cash = system.processFastagCrossing(plaza, fastagLane, "UNKNOWN",
                "CAR-2", VehicleType.CAR, cashLane, "operator", TIME.plusMinutes(5));
        check(cash.sentToCash() && cash.transactions().get(1).amount().equals(money("200")),
                "missing tag pays double in cash");

        system.registerVehicle(new Vehicle("CAR-3", VehicleType.CAR, "Owner Three", "9000000003", false));
        system.createTagAccount(ADMIN, "TAG-3", "CAR-3", money("500"));
        system.blacklistTag(ADMIN, "TAG-3", true);
        CrossingResult blocked = system.processFastagCrossing(plaza, fastagLane, "TAG-3",
                "CAR-3", VehicleType.CAR, cashLane, "operator", TIME.plusHours(1));
        check(!blocked.barrierOpened() && !blocked.sentToCash(), "blacklisted tags are refused");

        system.registerVehicle(new Vehicle("CAR-4", VehicleType.CAR, "Owner Four", "9000000004", false));
        system.createTagAccount(ADMIN, "TAG-4", "CAR-4", money("50"));
        system.recharge("Owner Four", "TAG-4", money("100"), PaymentMethod.UPI,
                PaymentStatus.PAID, TIME.plusHours(2));
        check(system.accountStatus("TAG-4") == AccountStatus.ACTIVE, "recharge reactivates blocked account");

        system.registerVehicle(new Vehicle("CAR-5", VehicleType.CAR, "Owner Five", "9000000005", false));
        system.createTagAccount(ADMIN, "TAG-5", "CAR-5", money("500"));
        TollTransaction charge = system.processFastagCrossing(plaza, fastagLane, "TAG-5",
                "CAR-5", VehicleType.CAR, cashLane, "operator", TIME.plusHours(3))
                .transactions().get(0);
        String dispute = system.submitDispute("Owner Five", charge.id(), "Incorrect charge", TIME.plusHours(4));
        system.resolveDispute(ADMIN, dispute, true, TIME.plusHours(5));
        check(system.tagBalance("TAG-5").equals(money("500")), "approved dispute refunds the charged tag");

        system.setRate(ADMIN, plaza, VehicleType.TRUCK, TripType.SINGLE, DATE.plusDays(1), money("150"));
        system.setRate(ADMIN, plaza, VehicleType.TRUCK, TripType.RETURN, DATE.plusDays(1), money("75"));
        system.registerVehicle(new Vehicle("CAR-6", VehicleType.CAR, "Owner Six", "9000000006", false));
        system.createTagAccount(ADMIN, "TAG-6", "CAR-6", money("500"));
        TollTransaction mismatch = system.processFastagCrossing(plaza, fastagLane, "TAG-6",
                "CAR-6", VehicleType.TRUCK, cashLane, "operator", TIME.plusDays(1))
                .transactions().get(0);
        check(mismatch.amount().equals(money("150")) && mismatch.flaggedForReview(),
                "mismatch charges the higher effective rate and is flagged");

        system.registerVehicle(new Vehicle("CAR-7", VehicleType.CAR, "Owner Seven", "9000000007", false));
        system.createTagAccount(ADMIN, "TAG-7", "CAR-7", money("100"));
        CrossingResult insufficient = system.processFastagCrossing(plaza, fastagLane, "TAG-7",
                "CAR-7", VehicleType.TRUCK, cashLane, "operator", TIME.plusDays(1).plusHours(1));
        check(insufficient.sentToCash()
                && insufficient.transactions().get(2).amount().equals(money("150")),
                "insufficient active tag balance falls back to normal cash toll");

        system.registerVehicle(new Vehicle("CAR-8", VehicleType.CAR, "Owner Eight", "9000000008", false));
        system.createTagAccount(ADMIN, "TAG-8", "CAR-8", money("1000"));
        system.setPassFee(ADMIN, VehicleType.CAR, money("300"));
        system.purchaseMonthlyPass("Owner Eight", "CAR-8", plaza, DATE, DATE.plusDays(29),
                "TAG-8", TIME.plusHours(6));
        TollTransaction passCrossing = system.processFastagCrossing(plaza, fastagLane, "TAG-8",
                "CAR-8", VehicleType.CAR, cashLane, "operator", TIME.plusHours(7))
                .transactions().get(0);
        check(passCrossing.amount().signum() == 0
                && system.dailyRevenue(plaza, DATE).compareTo(money("650")) == 0,
                "pass waives toll and its fee appears in net daily revenue");

        System.out.println("All toll-management checks passed.");
    }

    private static BigDecimal money(String amount) { return new BigDecimal(amount); }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
