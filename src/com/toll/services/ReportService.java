package com.toll.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.toll.models.FASTagAccount;
import com.toll.models.TollTransaction;
import com.toll.models.TollTypes.TransactionStatus;
import com.toll.repository.FASTagAccountRepository;
import com.toll.repository.TollTransactionRepository;

public class ReportService {
    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("200");

    private final TollTransactionRepository transactionRepository;
    private final FASTagAccountRepository accountRepository;

    public ReportService(
            TollTransactionRepository transactionRepository,
            FASTagAccountRepository accountRepository) {
        this.transactionRepository =
                Objects.requireNonNull(transactionRepository, "transactionRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
    }

    public Map<String, BigDecimal> dailyRevenueByPlaza(LocalDate date) {
        Objects.requireNonNull(date, "date");
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        for (TollTransaction transaction : transactionRepository.findAll()) {
            if (transaction.status() == TransactionStatus.SUCCESS
                    && transaction.time().toLocalDate().equals(date)) {
                revenue.merge(transaction.plazaId(), transaction.amount(), BigDecimal::add);
            }
        }
        return Map.copyOf(revenue);
    }

    public Map<String, Long> vehicleCountByLane(LocalDate date) {
        Objects.requireNonNull(date, "date");
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TollTransaction transaction : transactionRepository.findAll()) {
            if (transaction.time().toLocalDate().equals(date)
                    && transaction.laneId() != null
                    && transaction.status() != TransactionStatus.PENDING) {
                counts.merge(transaction.laneId(), 1L, Long::sum);
            }
        }
        return Map.copyOf(counts);
    }

    public Map<TransactionStatus, Long> transactionSummary(LocalDate date) {
        Objects.requireNonNull(date, "date");
        Map<TransactionStatus, Long> counts = new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus status : TransactionStatus.values()) {
            counts.put(status, 0L);
        }
        for (TollTransaction transaction : transactionRepository.findAll()) {
            if (transaction.time().toLocalDate().equals(date)) {
                counts.merge(transaction.status(), 1L, Long::sum);
            }
        }
        return Map.copyOf(counts);
    }

    public List<TollTransaction> failedTransactions(LocalDate from, LocalDate through) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(through, "through");
        if (through.isBefore(from)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.status() == TransactionStatus.FAILED
                        && !transaction.time().toLocalDate().isBefore(from)
                        && !transaction.time().toLocalDate().isAfter(through))
                .toList();
    }

    public List<FASTagAccount> lowBalanceAccounts() {
        List<FASTagAccount> accounts = new ArrayList<>();
        for (FASTagAccount account : accountRepository.findAll()) {
            if (account.balance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
                accounts.add(account);
            }
        }
        return List.copyOf(accounts);
    }
}
