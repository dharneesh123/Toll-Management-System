package com.toll;

import static com.toll.models.TollTypes.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.toll.models.Admin;
import com.toll.models.CrossingResult;
import com.toll.models.Dispute;
import com.toll.models.FASTagAccount;
import com.toll.models.Lane;
import com.toll.models.MonthlyPass;
import com.toll.models.Payment;
import com.toll.models.TollPlaza;
import com.toll.models.TollRate;
import com.toll.models.TollTransaction;
import com.toll.models.Vehicle;

public final class TollManagementSystem {
    private static final BigDecimal MINIMUM_RECHARGE = new BigDecimal("100");
    private static final BigDecimal MAXIMUM_RECHARGE = new BigDecimal("10000");
    private static final BigDecimal MAXIMUM_BALANCE = new BigDecimal("50000");
    private static final BigDecimal ALERT_THRESHOLD = new BigDecimal("200");

    private final Map<String, TollPlaza> plazas = new HashMap<>();
    private final Map<String, Vehicle> vehicles = new HashMap<>();
    private final Map<String, FASTagAccount> accounts = new HashMap<>();
    private final Map<String, String> activeTagByVehicle = new HashMap<>();
    private final List<TollTransaction> transactions = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();
    private final List<MonthlyPass> passes = new ArrayList<>();
    private final List<Dispute> disputes = new ArrayList<>();
    private final List<LowBalanceAlert> alerts = new ArrayList<>();
    private final Map<VehicleType, BigDecimal> passFees = new HashMap<>();

    public record LowBalanceAlert(String tagId, BigDecimal balance, LocalDateTime time) { }
    public record DisputeView(
            String id, String transactionId, String reason, LocalDateTime submittedAt,
            boolean resolved, boolean refunded) { }

    public synchronized String createPlaza(
            Admin admin, String name, String highway, String location, List<Lane> lanes) {
        requireAdmin(admin);
        Objects.requireNonNull(lanes, "lanes");
        if (lanes.size() < 2) throw new IllegalArgumentException("A plaza needs at least two lanes");
        TollPlaza plaza = new TollPlaza(name, highway, location);
        for (Lane lane : lanes) plaza.addLane(Objects.requireNonNull(lane, "lane").copy());
        plazas.put(plaza.id(), plaza);
        return plaza.id();
    }

    public synchronized String addLane(Admin admin, String plazaId, Lane lane) {
        requireAdmin(admin);
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        Lane managedLane = Objects.requireNonNull(lane, "lane").copy();
        plaza.addLane(managedLane);
        return managedLane.id();
    }

    public synchronized String laneId(String plazaId, int laneNumber) {
        Lane lane = required(plazas, plazaId, "plaza").laneByNumber(laneNumber);
        if (lane == null) throw new IllegalArgumentException("Lane not found");
        return lane.id();
    }

    public synchronized void setLaneStatus(
            Admin admin, String plazaId, String laneId, LaneStatus status) {
        requireAdmin(admin);
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        Lane lane = plaza.lane(laneId);
        if (lane == null) throw new IllegalArgumentException("Lane does not belong to this plaza");
        lane.setStatus(status);
    }

    public synchronized void setRate(
            Admin admin, String plazaId, VehicleType vehicleType, TripType tripType,
            LocalDate effectiveDate, BigDecimal amount) {
        requireAdmin(admin);
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        plaza.addRate(new TollRate(vehicleType, tripType, effectiveDate, amount));
    }

    public synchronized void openPlaza(Admin admin, String plazaId, LocalDate date) {
        requireAdmin(admin);
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        Objects.requireNonNull(date, "date");
        if (!plaza.hasRatesFor(date)) {
            throw new IllegalStateException("Every vehicle type and trip type needs an effective rate");
        }
        plaza.setOpen(true);
    }

    public synchronized void registerVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        if (vehicles.putIfAbsent(vehicle.registration(), vehicle) != null) {
            throw new IllegalArgumentException("Vehicle registration already exists");
        }
    }

    public synchronized void createTagAccount(
            Admin admin, String tagId, String vehicleRegistration, BigDecimal initialBalance) {
        requireAdmin(admin);
        requireText(tagId, "tag ID");
        required(vehicles, vehicleRegistration, "vehicle");
        requireNonNegative(initialBalance, "initial balance");
        if (initialBalance.compareTo(MAXIMUM_BALANCE) > 0) {
            throw new IllegalArgumentException("Balance cannot exceed Rs. 50,000");
        }
        if (accounts.containsKey(tagId)) throw new IllegalArgumentException("Tag ID already exists");
        String oldTag = activeTagByVehicle.get(vehicleRegistration);
        if (oldTag != null && accounts.get(oldTag).status() != AccountStatus.CLOSED) {
            throw new IllegalStateException("Vehicle already has an active tag");
        }
        FASTagAccount account = new FASTagAccount(tagId, vehicleRegistration, initialBalance);
        accounts.put(tagId, account);
        activeTagByVehicle.put(vehicleRegistration, tagId);
    }

    public synchronized void setAccountStatus(Admin admin, String tagId, AccountStatus status) {
        requireAdmin(admin);
        FASTagAccount account = required(accounts, tagId, "tag");
        account.setStatus(status);
        if (status == AccountStatus.CLOSED) {
            activeTagByVehicle.remove(account.vehicleRegistration(), tagId);
        }
    }

    public synchronized void blacklistTag(Admin admin, String tagId, boolean blacklisted) {
        requireAdmin(admin);
        required(accounts, tagId, "tag").setBlacklisted(blacklisted);
    }

    public synchronized Payment recharge(
            String ownerName, String tagId, BigDecimal amount,
            PaymentMethod method, PaymentStatus status, LocalDateTime time) {
        FASTagAccount account = required(accounts, tagId, "tag");
        Vehicle vehicle = required(vehicles, account.vehicleRegistration(), "vehicle");
        if (!vehicle.ownerName().equals(ownerName)) throw new SecurityException("Tag belongs to another owner");
        if (account.status() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Closed tag accounts cannot be recharged");
        }
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(time, "time");
        if (method != PaymentMethod.UPI && method != PaymentMethod.CARD
                && method != PaymentMethod.NET_BANKING) {
            throw new IllegalArgumentException("Recharge method must be UPI, Card, or Net Banking");
        }
        requireNonNegative(amount, "recharge amount");
        if (amount.compareTo(MINIMUM_RECHARGE) < 0 || amount.compareTo(MAXIMUM_RECHARGE) > 0) {
            throw new IllegalArgumentException("Recharge must be between Rs. 100 and Rs. 10,000");
        }
        if (status == PaymentStatus.PAID
                && account.balance().add(amount).compareTo(MAXIMUM_BALANCE) > 0) {
            throw new IllegalArgumentException("Balance cannot exceed Rs. 50,000");
        }
        Payment payment = new Payment(PaymentType.RECHARGE, tagId, amount, time, method, status, null);
        payments.add(payment);
        if (status == PaymentStatus.PAID) {
            account.credit(amount);
        }
        return payment;
    }

    public synchronized void setPassFee(Admin admin, VehicleType type, BigDecimal fee) {
        requireAdmin(admin);
        Objects.requireNonNull(type, "type");
        requireNonNegative(fee, "pass fee");
        passFees.put(type, fee);
    }

    public synchronized String purchaseMonthlyPass(
            String ownerName, String vehicleRegistration, String plazaId,
            LocalDate startDate, LocalDate endDate, String tagId, LocalDateTime purchasedAt) {
        Vehicle vehicle = required(vehicles, vehicleRegistration, "vehicle");
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        if (!vehicle.ownerName().equals(ownerName)) throw new SecurityException("Vehicle belongs to another owner");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(purchasedAt, "purchasedAt");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("Pass end date precedes start date");
        boolean overlaps = passes.stream().anyMatch(pass ->
                pass.vehicleRegistration().equals(vehicleRegistration)
                        && pass.plazaId().equals(plazaId)
                        && !endDate.isBefore(pass.startDate())
                        && !startDate.isAfter(pass.endDate()));
        if (overlaps) throw new IllegalStateException("Vehicle already has a valid pass for this plaza");
        BigDecimal fee = required(passFees, vehicle.type(), "pass fee for " + vehicle.type());
        FASTagAccount account = required(accounts, tagId, "tag");
        if (!account.vehicleRegistration().equals(vehicleRegistration)
                || account.status() != AccountStatus.ACTIVE || account.blacklisted()) {
            throw new IllegalStateException("An active, non-blacklisted vehicle tag is required");
        }
        if (account.balance().compareTo(fee) < 0) throw new IllegalStateException("Insufficient tag balance");
        account.debit(fee);
        if (fee.signum() > 0) checkLowBalance(account, purchasedAt);
        MonthlyPass pass = new MonthlyPass(vehicleRegistration, plaza.id(), startDate, endDate,
                fee, purchasedAt.toLocalDate());
        passes.add(pass);
        payments.add(new Payment(PaymentType.PASS, pass.id(), fee, purchasedAt,
                PaymentMethod.FASTAG, PaymentStatus.PAID, null));
        return pass.id();
    }

    public synchronized CrossingResult processFastagCrossing(
            String plazaId, String fastagLaneId, String tagId, String observedRegistration,
            VehicleType observedType, String cashLaneId, String laneOperator, LocalDateTime time) {
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        Lane fastagLane = openLane(plaza, fastagLaneId, LaneType.FASTAG);
        Objects.requireNonNull(observedType, "observedType");
        Objects.requireNonNull(time, "time");
        fastagLane.beginProcessing();
        try {
            FASTagAccount account = accounts.get(tagId);
            if (account != null && account.blacklisted()) {
                TollTransaction failed = record(account.vehicleRegistration(), plaza, fastagLane, time, BigDecimal.ZERO,
                        TransactionStatus.FAILED, TripType.SINGLE, false, account.tagId(),
                        "Blacklisted tag refused");
                return new CrossingResult(List.of(failed), false, false);
            }

            Vehicle vehicle = account == null
                    ? vehicles.get(observedRegistration)
                    : vehicles.get(account.vehicleRegistration());
            boolean usableTag = account != null && account.status() == AccountStatus.ACTIVE && vehicle != null;
            if (!usableTag) {
                if (vehicle == null) {
                    throw new IllegalArgumentException("A registered vehicle is required for cash fallback");
                }
                return fallbackToCash(plaza, fastagLane, vehicle, observedType, cashLaneId,
                        laneOperator, time, "No valid active FASTag", true);
            }
            if (vehicle.emergency()) {
                throw new IllegalStateException("Emergency vehicles must use an emergency lane");
            }

            TollTransaction previous = latestSuccessfulTransaction(vehicle.registration(), plaza.id());
            TripType trip = isReturn(previous, time) ? TripType.RETURN : TripType.SINGLE;
            BigDecimal toll = tollFor(plaza, vehicle.type(), observedType, trip, time.toLocalDate());
            if (hasRecentTagCharge(account.tagId(), plaza.id(), time)) {
                TollTransaction failed = record(vehicle.registration(), plaza, fastagLane, time,
                        BigDecimal.ZERO, TransactionStatus.FAILED, trip, false, account.tagId(),
                        "Same tag was charged at this plaza within two minutes");
                return new CrossingResult(List.of(failed), false, false);
            }
            if (hasValidPass(vehicle.registration(), plaza.id(), time.toLocalDate())) {
                boolean review = vehicle.type() != observedType;
                TollTransaction passed = record(vehicle.registration(), plaza, fastagLane, time,
                        BigDecimal.ZERO, TransactionStatus.SUCCESS, trip, review, account.tagId(),
                        review ? "Valid monthly pass; observed vehicle type mismatch flagged" : "Valid monthly pass");
                return new CrossingResult(List.of(passed), true, false);
            }
            if (account.balance().compareTo(toll) < 0) {
                TollTransaction failed = record(vehicle.registration(), plaza, fastagLane, time,
                        toll, TransactionStatus.FAILED, trip, false, account.tagId(),
                        "Insufficient FASTag balance");
                CrossingResult cash = fallbackToCash(plaza, fastagLane, vehicle, observedType,
                        cashLaneId, laneOperator, time, "Insufficient FASTag balance", false);
                List<TollTransaction> result = new ArrayList<>();
                result.add(failed);
                result.addAll(cash.transactions());
                return new CrossingResult(result, cash.barrierOpened(), true);
            }

            boolean review = vehicle.type() != observedType;
            account.debit(toll);
            checkLowBalance(account, time);
            TollTransaction success = record(vehicle.registration(), plaza, fastagLane, time, toll,
                    TransactionStatus.SUCCESS, trip, review, account.tagId(),
                    review ? "Observed vehicle type differs; higher toll charged" : "FASTag payment");
            return new CrossingResult(List.of(success), true, false);
        } finally {
            fastagLane.endProcessing();
        }
    }

    public synchronized TollTransaction processEmergencyCrossing(
            String plazaId, String emergencyLaneId, String vehicleRegistration, LocalDateTime time) {
        Objects.requireNonNull(time, "time");
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        Lane lane = openLane(plaza, emergencyLaneId, LaneType.EMERGENCY);
        Vehicle vehicle = required(vehicles, vehicleRegistration, "vehicle");
        if (!vehicle.emergency()) throw new IllegalStateException("Vehicle is not registered as an emergency vehicle");
        lane.beginProcessing();
        try {
            return record(vehicle.registration(), plaza, lane, time, BigDecimal.ZERO,
                    TransactionStatus.SUCCESS, TripType.SINGLE, false, null, "Emergency vehicle exempt");
        } finally {
            lane.endProcessing();
        }
    }

    public synchronized TollTransaction processCashCrossing(
            String plazaId, String cashLaneId, String vehicleRegistration,
            VehicleType observedType, String laneOperator, LocalDateTime time) {
        return processCashCrossing(plazaId, cashLaneId, vehicleRegistration,
                observedType, laneOperator, time, false);
    }

    private TollTransaction processCashCrossing(
            String plazaId, String cashLaneId, String vehicleRegistration,
            VehicleType observedType, String laneOperator, LocalDateTime time, boolean doubleToll) {
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        Lane lane = openLane(plaza, cashLaneId, LaneType.CASH);
        Vehicle vehicle = required(vehicles, vehicleRegistration, "vehicle");
        if (vehicle.emergency()) throw new IllegalStateException("Emergency vehicles must use an emergency lane");
        requireText(laneOperator, "lane operator");
        Objects.requireNonNull(observedType, "observedType");
        Objects.requireNonNull(time, "time");
        lane.beginProcessing();
        try {
            TollTransaction previous = latestSuccessfulTransaction(vehicle.registration(), plaza.id());
            TripType trip = isReturn(previous, time) ? TripType.RETURN : TripType.SINGLE;
            if (hasValidPass(vehicle.registration(), plaza.id(), time.toLocalDate())) {
                boolean review = vehicle.type() != observedType;
                return record(vehicle.registration(), plaza, lane, time, BigDecimal.ZERO,
                        TransactionStatus.SUCCESS, trip, review, null,
                        review ? "Valid monthly pass; observed vehicle type mismatch flagged" : "Valid monthly pass");
            }
            BigDecimal amount = tollAmount(plaza, vehicle.type(), observedType, trip, time.toLocalDate());
            if (doubleToll) amount = amount.multiply(new BigDecimal("2"));
            amount = roundToNearestFive(amount);
            TollTransaction transaction = record(vehicle.registration(), plaza, lane, time, amount,
                    TransactionStatus.SUCCESS, trip, vehicle.type() != observedType, null, "Cash payment");
            payments.add(new Payment(PaymentType.CASH_TOLL, transaction.id(), amount, time,
                    PaymentMethod.CASH, PaymentStatus.PAID, laneOperator));
            return transaction;
        } finally {
            lane.endProcessing();
        }
    }

    public synchronized String submitDispute(
            String ownerName, String transactionId, String reason, LocalDateTime submittedAt) {
        requireText(ownerName, "owner name");
        requireText(reason, "dispute reason");
        Objects.requireNonNull(submittedAt, "submittedAt");
        TollTransaction transaction = findTransaction(transactionId);
        Vehicle vehicle = required(vehicles, transaction.vehicleRegistration(), "vehicle");
        if (!vehicle.ownerName().equals(ownerName)) {
            throw new SecurityException("Transaction belongs to another owner");
        }
        if (transaction.status() != TransactionStatus.SUCCESS || transaction.amount().signum() <= 0
                || transaction.tagId() == null) {
            throw new IllegalStateException("Only successful FASTag deductions can be disputed");
        }
        if (disputes.stream().anyMatch(dispute -> dispute.transactionId().equals(transactionId))) {
            throw new IllegalStateException("A dispute already exists for this transaction");
        }
        if (submittedAt.isBefore(transaction.time())
                || Duration.between(transaction.time(), submittedAt).compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalStateException("Disputes must be submitted within seven days");
        }
        Dispute dispute = new Dispute(transactionId, reason, submittedAt);
        disputes.add(dispute);
        return dispute.id();
    }

    public synchronized void resolveDispute(
            Admin admin, String disputeId, boolean approved, LocalDateTime resolvedAt) {
        requireAdmin(admin);
        Dispute dispute = disputes.stream().filter(item -> item.id().equals(disputeId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        Objects.requireNonNull(resolvedAt, "resolvedAt");
        if (resolvedAt.isBefore(dispute.submittedAt())) {
            throw new IllegalArgumentException("Resolution cannot precede dispute submission");
        }
        if (dispute.resolved()) throw new IllegalStateException("Dispute has already been resolved");
        if (approved) {
            TollTransaction transaction = findTransaction(dispute.transactionId());
            FASTagAccount account = required(accounts, transaction.tagId(), "tag");
            if (account.balance().add(transaction.amount()).compareTo(MAXIMUM_BALANCE) > 0) {
                throw new IllegalStateException("Refund would exceed the maximum tag balance");
            }
            account.credit(transaction.amount());
            payments.add(new Payment(PaymentType.REFUND, transaction.id(), transaction.amount(),
                    resolvedAt, PaymentMethod.FASTAG, PaymentStatus.REFUNDED, null));
        }
        dispute.resolve(approved);
    }

    public synchronized BigDecimal dailyRevenue(String plazaId, LocalDate date) {
        required(plazas, plazaId, "plaza");
        BigDecimal tollRevenue = transactions.stream()
                .filter(tx -> tx.plazaId().equals(plazaId) && tx.time().toLocalDate().equals(date)
                        && tx.status() == TransactionStatus.SUCCESS)
                .map(TollTransaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal passRevenue = payments.stream()
                .filter(payment -> payment.type() == PaymentType.PASS
                        && payment.time().toLocalDate().equals(date))
                .filter(payment -> passes.stream().anyMatch(pass ->
                        pass.id().equals(payment.referenceId()) && pass.plazaId().equals(plazaId)))
                .map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = payments.stream()
                .filter(payment -> payment.type() == PaymentType.REFUND
                        && payment.time().toLocalDate().equals(date))
                .filter(payment -> transactions.stream().anyMatch(tx ->
                        tx.id().equals(payment.referenceId()) && tx.plazaId().equals(plazaId)))
                .map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return tollRevenue.add(passRevenue).subtract(refunds);
    }

    public synchronized long vehicleCountForLane(String plazaId, String laneId, LocalDate date) {
        TollPlaza plaza = required(plazas, plazaId, "plaza");
        if (!plaza.containsLane(laneId)) throw new IllegalArgumentException("Lane does not belong to this plaza");
        return transactions.stream()
                .filter(tx -> tx.plazaId().equals(plazaId) && tx.laneId().equals(laneId)
                        && tx.time().toLocalDate().equals(date)
                        && tx.status() == TransactionStatus.SUCCESS)
                .count();
    }

    public synchronized BigDecimal tagBalance(String tagId) {
        return required(accounts, tagId, "tag").balance();
    }

    public synchronized AccountStatus accountStatus(String tagId) {
        return required(accounts, tagId, "tag").status();
    }

    public synchronized List<TollTransaction> transactions() {
        return List.copyOf(transactions);
    }

    public synchronized List<Payment> payments() {
        return List.copyOf(payments);
    }

    public synchronized List<MonthlyPass> passes() {
        return List.copyOf(passes);
    }

    public synchronized List<DisputeView> disputes() {
        return disputes.stream().map(dispute -> new DisputeView(dispute.id(), dispute.transactionId(),
                dispute.reason(), dispute.submittedAt(), dispute.resolved(), dispute.refunded())).toList();
    }

    public synchronized List<LowBalanceAlert> lowBalanceAlerts() {
        return List.copyOf(alerts);
    }

    private CrossingResult fallbackToCash(
            TollPlaza plaza, Lane fastagLane, Vehicle vehicle, VehicleType observedType,
            String cashLaneId, String laneOperator, LocalDateTime time, String reason, boolean doubleToll) {
        TollTransaction failed = record(vehicle.registration(), plaza, fastagLane, time,
                BigDecimal.ZERO, TransactionStatus.FAILED, TripType.SINGLE, false, null, reason);
        TollTransaction cash = processCashCrossing(plaza.id(), cashLaneId, vehicle.registration(),
                observedType, laneOperator, time, doubleToll);
        return new CrossingResult(List.of(failed, cash), true, true);
    }

    private TollTransaction record(
            String registration, TollPlaza plaza, Lane lane, LocalDateTime time,
            BigDecimal amount, TransactionStatus status, TripType trip, boolean review,
            String tagId, String details) {
        TollTransaction transaction = new TollTransaction(UUID.randomUUID().toString(),
                registration, plaza.id(), lane.id(), time, amount, status, trip, review, tagId, details);
        transactions.add(transaction);
        return transaction;
    }

    private BigDecimal tollFor(
            TollPlaza plaza, VehicleType registered, VehicleType observed, TripType trip, LocalDate date) {
        return roundToNearestFive(tollAmount(plaza, registered, observed, trip, date));
    }

    private BigDecimal tollAmount(
            TollPlaza plaza, VehicleType registered, VehicleType observed, TripType trip, LocalDate date) {
        BigDecimal registeredRate = tollForType(plaza, registered, trip, date);
        BigDecimal observedRate = tollForType(plaza, observed, trip, date);
        return registeredRate.max(observedRate);
    }

    private static BigDecimal roundToNearestFive(BigDecimal amount) {
        return amount.divide(new BigDecimal("5"), 0, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("5"));
    }

    private BigDecimal tollForType(TollPlaza plaza, VehicleType type, TripType trip, LocalDate date) {
        TollRate selectedRate = plaza.rateAt(type, trip, date);
        if (selectedRate == null) throw new IllegalStateException("No effective toll rate for " + type + " " + trip);
        if (trip == TripType.RETURN) {
            TollRate singleRate = plaza.rateAt(type, TripType.SINGLE, date);
            if (singleRate == null) throw new IllegalStateException("No effective single-trip rate for " + type);
            return singleRate.amount().divide(new BigDecimal("2"));
        }
        return selectedRate.amount();
    }

    private boolean hasRecentTagCharge(String tagId, String plazaId, LocalDateTime time) {
        return transactions.stream().anyMatch(tx ->
                tx.status() == TransactionStatus.SUCCESS && tx.amount().signum() > 0
                        && tx.plazaId().equals(plazaId)
                        && tx.time().isAfter(time.minusMinutes(2)) && !tx.time().isAfter(time)
                        && tagId.equals(tx.tagId()));
    }

    private TollTransaction latestSuccessfulTransaction(String registration, String plazaId) {
        return transactions.stream()
                .filter(tx -> Objects.equals(tx.vehicleRegistration(), registration)
                        && tx.plazaId().equals(plazaId) && tx.status() == TransactionStatus.SUCCESS)
                .max(Comparator.comparing(TollTransaction::time)).orElse(null);
    }

    private static boolean isReturn(TollTransaction previous, LocalDateTime time) {
        return previous != null && previous.tripType() == TripType.SINGLE
                && !previous.time().isAfter(time)
                && Duration.between(previous.time(), time).compareTo(Duration.ofHours(24)) <= 0;
    }

    private boolean hasValidPass(String registration, String plazaId, LocalDate date) {
        return passes.stream().anyMatch(pass -> pass.vehicleRegistration().equals(registration)
                && pass.plazaId().equals(plazaId)
                && !date.isBefore(pass.startDate()) && !date.isAfter(pass.endDate()));
    }

    private void checkLowBalance(FASTagAccount account, LocalDateTime time) {
        if (account.balance().compareTo(ALERT_THRESHOLD) < 0) {
            alerts.add(new LowBalanceAlert(account.tagId(), account.balance(), time));
        }
    }

    private static Lane openLane(TollPlaza plaza, String laneId, LaneType requiredType) {
        if (!plaza.isOpen()) throw new IllegalStateException("Plaza is not open");
        Lane lane = plaza.lane(laneId);
        if (lane == null || lane.type() != requiredType) {
            throw new IllegalArgumentException("Lane is not a " + requiredType + " lane in this plaza");
        }
        if (lane.status() != LaneStatus.OPEN) throw new IllegalStateException("Lane is closed");
        return lane;
    }

    private TollTransaction findTransaction(String id) {
        return transactions.stream().filter(tx -> tx.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    private static void requireAdmin(Admin admin) {
        if (admin == null) throw new SecurityException("Admin role required");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.signum() < 0) throw new IllegalArgumentException(field + " cannot be negative");
    }

    private static <K, V> V required(Map<K, V> map, K key, String field) {
        V value = map.get(key);
        if (value == null) throw new IllegalArgumentException(field + " not found");
        return value;
    }
}
