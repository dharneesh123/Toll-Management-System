package com.toll.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import com.toll.models.CrossingResult;
import com.toll.models.FASTagAccount;
import com.toll.models.Lane;
import com.toll.models.MonthlyPass;
import com.toll.models.TollPlaza;
import com.toll.models.TollTransaction;
import com.toll.models.TollTypes.AccountStatus;
import com.toll.models.TollTypes.LaneType;
import com.toll.models.TollTypes.PaymentMethod;
import com.toll.models.TollTypes.TransactionStatus;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;
import com.toll.models.Vehicle;
import com.toll.repository.FASTagAccountRepository;
import com.toll.repository.TollPlazaRepository;
import com.toll.repository.TollTransactionRepository;
import com.toll.repository.VehicleRepository;

public class TollProcessingService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final TollPlazaRepository tollPlazaRepository;
    private final VehicleRepository vehicleRepository;
    private final FASTagAccountRepository accountRepository;
    private final TollTransactionRepository transactionRepository;
    private final TollRateService tollRateService;
    private final MonthlyPassService monthlyPassService;
    private final PaymentService paymentService;

    public TollProcessingService(
            TollPlazaRepository tollPlazaRepository,
            VehicleRepository vehicleRepository,
            FASTagAccountRepository accountRepository,
            TollTransactionRepository transactionRepository,
            TollRateService tollRateService,
            MonthlyPassService monthlyPassService,
            PaymentService paymentService) {
        this.tollPlazaRepository = Objects.requireNonNull(tollPlazaRepository, "tollPlazaRepository");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.transactionRepository =
                Objects.requireNonNull(transactionRepository, "transactionRepository");
        this.tollRateService = Objects.requireNonNull(tollRateService, "tollRateService");
        this.monthlyPassService = Objects.requireNonNull(monthlyPassService, "monthlyPassService");
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
    }

    public CrossingResult processCrossing(
            String vehicleRegistration,
            String tagId,
            String plazaId,
            String fastagLaneId,
            String cashLaneId,
            String emergencyLaneId,
            VehicleType observedVehicleType,
            LocalDateTime time,
            String laneOperator) {
        requireText(vehicleRegistration, "Vehicle registration");
        requireText(plazaId, "Plaza ID");
        Objects.requireNonNull(observedVehicleType, "observedVehicleType");
        Objects.requireNonNull(time, "time");

        TollPlaza plaza = tollPlazaRepository.findById(plazaId);
        if (plaza == null) {
            throw new IllegalArgumentException("Toll plaza not found: " + plazaId);
        }
        Vehicle vehicle = vehicleRepository.findByRegistration(vehicleRegistration);
        boolean emergency = vehicle != null && vehicle.emergency();
        FASTagAccount account = tagId == null || tagId.isBlank()
                ? null : accountRepository.findById(tagId);
        boolean validTag = account != null
                && account.vehicleRegistration().equals(vehicleRegistration)
                && account.status() == AccountStatus.ACTIVE
                && !account.blacklisted();
        boolean sentToCash = !emergency && !validTag;
        String selectedLaneId = emergency
                ? emergencyLaneId
                : sentToCash ? cashLaneId : fastagLaneId;
        TripType tripType = determineTripType(vehicleRegistration, plazaId, time);

        if (!plaza.isOpen()) {
            return failedCrossing(vehicleRegistration, plazaId, selectedLaneId, time, tagId,
                    tripType, false, "Toll plaza is closed");
        }
        if (isDuplicateTagCrossing(tagId, plazaId, time)) {
            return failedCrossing(vehicleRegistration, plazaId, selectedLaneId, time, tagId,
                    tripType, sentToCash, "Duplicate tag crossing within two minutes");
        }
        Lane lane = selectedLaneId == null || selectedLaneId.isBlank()
                ? null : plaza.lane(selectedLaneId);
        if (lane == null) {
            return failedCrossing(vehicleRegistration, plazaId, selectedLaneId, time, tagId,
                    tripType, sentToCash, "Required lane was not found in the plaza");
        }
        LaneType expectedLaneType = emergency
                ? LaneType.EMERGENCY
                : sentToCash ? LaneType.CASH : LaneType.FASTAG;
        if (lane.type() != expectedLaneType) {
            return failedCrossing(vehicleRegistration, plazaId, lane.id(), time, tagId,
                    tripType, sentToCash, "Lane type does not match the required processing route");
        }

        boolean mismatch = vehicle != null && vehicle.type() != observedVehicleType;
        MonthlyPass pass = !emergency && validTag
                ? monthlyPassService.findValidPass(
                        vehicleRegistration, plazaId, time.toLocalDate())
                : null;
        BigDecimal amount = ZERO;
        if (!emergency && pass == null) {
            amount = calculateCharge(
                    plazaId, vehicle, observedVehicleType, tripType, time.toLocalDate(), mismatch);
            if (sentToCash) {
                amount = amount.multiply(new BigDecimal("2"));
            }
        }

        if (sentToCash && (laneOperator == null || laneOperator.isBlank())) {
            return failedCrossing(vehicleRegistration, plazaId, lane.id(), time, tagId,
                    tripType, true, "Lane operator is required for cash-lane processing");
        }

        try {
            lane.beginProcessing();
        } catch (IllegalStateException exception) {
            return failedCrossing(vehicleRegistration, plazaId, lane.id(), time, tagId,
                    tripType, sentToCash, exception.getMessage());
        }

        try {
            if (validTag && !emergency && pass == null) {
                if (account.balance().compareTo(amount) < 0) {
                    lane.endProcessing();
                    return processInsufficientBalanceOnCash(
                            vehicleRegistration, tagId, plaza, cashLaneId, observedVehicleType,
                            time, laneOperator, vehicle, tripType, mismatch);
                }
                account.debit(amount);
                accountRepository.save(account);
            }

            TollTransaction transaction = new TollTransaction(
                    java.util.UUID.randomUUID().toString(),
                    vehicleRegistration,
                    plazaId,
                    lane.id(),
                    time,
                    amount,
                    TransactionStatus.SUCCESS,
                    tripType,
                    mismatch,
                    tagId,
                    transactionDetails(emergency, pass, sentToCash, mismatch));
            transactionRepository.save(transaction);

            if (!emergency) {
                if (pass != null) {
                    paymentService.recordPassPayment(transaction.id(), amount, time);
                } else {
                    paymentService.recordTollPayment(
                            transaction.id(), amount,
                            sentToCash ? PaymentMethod.CASH : PaymentMethod.FASTAG,
                            time, sentToCash ? laneOperator : null);
                }
            }
            return new CrossingResult(List.of(transaction), true, sentToCash);
        } finally {
            lane.endProcessing();
        }
    }

    private CrossingResult processInsufficientBalanceOnCash(
            String vehicleRegistration,
            String tagId,
            TollPlaza plaza,
            String cashLaneId,
            VehicleType observedVehicleType,
            LocalDateTime time,
            String laneOperator,
            Vehicle vehicle,
            TripType tripType,
            boolean mismatch) {
        Lane cashLane = cashLaneId == null || cashLaneId.isBlank()
                ? null : plaza.lane(cashLaneId);
        if (cashLane == null || cashLane.type() != LaneType.CASH) {
            return failedCrossing(vehicleRegistration, plaza.id(), cashLaneId, time, tagId,
                    tripType, true, "A valid CASH lane is required for insufficient balance");
        }
        if (laneOperator == null || laneOperator.isBlank()) {
            return failedCrossing(vehicleRegistration, plaza.id(), cashLane.id(), time, tagId,
                    tripType, true, "Lane operator is required for cash-lane processing");
        }

        BigDecimal amount = calculateCharge(
                plaza.id(), vehicle, observedVehicleType, tripType, time.toLocalDate(), mismatch);
        try {
            cashLane.beginProcessing();
        } catch (IllegalStateException exception) {
            return failedCrossing(vehicleRegistration, plaza.id(), cashLane.id(), time, tagId,
                    tripType, true, exception.getMessage());
        }
        try {
            TollTransaction transaction = new TollTransaction(
                    java.util.UUID.randomUUID().toString(),
                    vehicleRegistration,
                    plaza.id(),
                    cashLane.id(),
                    time,
                    amount,
                    TransactionStatus.SUCCESS,
                    tripType,
                    mismatch,
                    tagId,
                    transactionDetails(false, null, true, mismatch)
                            + "; insufficient FASTag balance");
            transactionRepository.save(transaction);
            paymentService.recordTollPayment(
                    transaction.id(), amount, PaymentMethod.CASH, time, laneOperator);
            return new CrossingResult(List.of(transaction), true, true);
        } finally {
            cashLane.endProcessing();
        }
    }

    private BigDecimal calculateCharge(
            String plazaId,
            Vehicle vehicle,
            VehicleType observedType,
            TripType tripType,
            LocalDate date,
            boolean mismatch) {
        BigDecimal observedAmount =
                tollRateService.calculateTollAmount(plazaId, observedType, tripType, date);
        if (!mismatch || vehicle == null) {
            return observedAmount;
        }
        BigDecimal registeredAmount =
                tollRateService.calculateTollAmount(plazaId, vehicle.type(), tripType, date);
        return registeredAmount.max(observedAmount);
    }

    private TripType determineTripType(
            String vehicleRegistration, String plazaId, LocalDateTime time) {
        TollTransaction previous = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.status() == TransactionStatus.SUCCESS
                        && transaction.amount().signum() > 0
                        && transaction.vehicleRegistration().equals(vehicleRegistration)
                        && transaction.plazaId().equals(plazaId)
                        && !transaction.time().isAfter(time)
                        && !transaction.time().isBefore(time.minusHours(24)))
                .max((first, second) -> first.time().compareTo(second.time()))
                .orElse(null);
        return previous != null && previous.tripType() == TripType.SINGLE
                ? TripType.RETURN : TripType.SINGLE;
    }

    private boolean isDuplicateTagCrossing(String tagId, String plazaId, LocalDateTime time) {
        if (tagId == null || tagId.isBlank()) {
            return false;
        }
        LocalDateTime windowStart = time.minusMinutes(2);
        return transactionRepository.findAll().stream()
                .anyMatch(transaction -> transaction.status() == TransactionStatus.SUCCESS
                        && tagId.equals(transaction.tagId())
                        && plazaId.equals(transaction.plazaId())
                        && !transaction.time().isAfter(time)
                        && !transaction.time().isBefore(windowStart));
    }

    private CrossingResult failedCrossing(
            String registration,
            String plazaId,
            String laneId,
            LocalDateTime time,
            String tagId,
            TripType tripType,
            boolean sentToCash,
            String reason) {
        TollTransaction transaction = new TollTransaction(
                java.util.UUID.randomUUID().toString(),
                registration,
                plazaId,
                laneId,
                time,
                ZERO,
                TransactionStatus.FAILED,
                tripType,
                false,
                tagId,
                reason);
        transactionRepository.save(transaction);
        return new CrossingResult(List.of(transaction), false, sentToCash);
    }

    private static String transactionDetails(
            boolean emergency, MonthlyPass pass, boolean sentToCash, boolean mismatch) {
        if (emergency) {
            return "Emergency vehicle; zero toll";
        }
        if (pass != null) {
            return "Valid monthly pass; zero toll";
        }
        if (mismatch) {
            return "Vehicle type mismatch; higher applicable toll charged; flagged for review";
        }
        return sentToCash ? "Cash-lane toll payment" : "FASTag toll payment";
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
