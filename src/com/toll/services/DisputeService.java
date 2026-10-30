package com.toll.services;

import java.time.LocalDateTime;
import java.util.Objects;
import com.toll.models.Dispute;
import com.toll.models.FASTagAccount;
import com.toll.models.TollTransaction;
import com.toll.models.TollTypes.TransactionStatus;
import com.toll.repository.AdminRepository;
import com.toll.repository.DisputeRepository;
import com.toll.repository.FASTagAccountRepository;
import com.toll.repository.TollTransactionRepository;

public class DisputeService {
    private final DisputeRepository disputeRepository;
    private final TollTransactionRepository transactionRepository;
    private final FASTagAccountRepository accountRepository;
    private final AdminRepository adminRepository;
    private final PaymentService paymentService;

    public DisputeService(
            DisputeRepository disputeRepository,
            TollTransactionRepository transactionRepository,
            FASTagAccountRepository accountRepository,
            AdminRepository adminRepository,
            PaymentService paymentService) {
        this.disputeRepository = Objects.requireNonNull(disputeRepository, "disputeRepository");
        this.transactionRepository =
                Objects.requireNonNull(transactionRepository, "transactionRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.adminRepository = Objects.requireNonNull(adminRepository, "adminRepository");
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
    }

    public Dispute submitDispute(
            String vehicleRegistration,
            String transactionId,
            String reason,
            LocalDateTime submittedAt) {
        requireText(vehicleRegistration, "Vehicle registration");
        requireText(transactionId, "Transaction ID");
        Objects.requireNonNull(submittedAt, "submittedAt");

        TollTransaction transaction = transactionRepository.findById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        if (!transaction.vehicleRegistration().equals(vehicleRegistration)) {
            throw new SecurityException("Only the vehicle owner may dispute this transaction");
        }
        if (transaction.status() != TransactionStatus.SUCCESS) {
            throw new IllegalStateException("Only successful toll transactions can be disputed");
        }
        if (submittedAt.isBefore(transaction.time())
                || submittedAt.isAfter(transaction.time().plusDays(7))) {
            throw new IllegalArgumentException("Disputes must be submitted within seven days");
        }
        if (disputeRepository.findByTransactionId(transactionId) != null) {
            throw new IllegalStateException("A dispute already exists for this transaction");
        }

        Dispute dispute = new Dispute(transactionId, reason, submittedAt);
        disputeRepository.save(dispute);
        return dispute;
    }

    public Dispute resolveDispute(
            String adminName, String disputeId, boolean approve, LocalDateTime resolvedAt) {
        requireAdmin(adminName);
        requireText(disputeId, "Dispute ID");
        Objects.requireNonNull(resolvedAt, "resolvedAt");

        Dispute dispute = disputeRepository.findById(disputeId);
        if (dispute == null) {
            throw new IllegalArgumentException("Dispute not found: " + disputeId);
        }
        if (dispute.resolved()) {
            throw new IllegalStateException("Dispute has already been resolved");
        }
        if (resolvedAt.isBefore(dispute.submittedAt())) {
            throw new IllegalArgumentException("Resolution time cannot precede dispute submission");
        }

        TollTransaction transaction = transactionRepository.findById(dispute.transactionId());
        if (transaction == null) {
            throw new IllegalStateException("Disputed transaction no longer exists");
        }
        if (approve && transaction.amount().signum() > 0) {
            FASTagAccount account = accountFor(transaction);
            paymentService.refundTollToAccount(
                    account.tagId(), transaction.id(), transaction.amount(), resolvedAt);
        }

        dispute.resolve(approve);
        disputeRepository.save(dispute);
        return dispute;
    }

    private FASTagAccount accountFor(TollTransaction transaction) {
        FASTagAccount account = transaction.tagId() == null || transaction.tagId().isBlank()
                ? null : accountRepository.findById(transaction.tagId());
        if (account == null) {
            account = accountRepository.findNonClosedByVehicleRegistration(
                    transaction.vehicleRegistration());
        }
        if (account == null
                || !account.vehicleRegistration().equals(transaction.vehicleRegistration())) {
            throw new IllegalStateException(
                    "No FASTag account is available for the approved refund");
        }
        return account;
    }

    private void requireAdmin(String adminName) {
        if (adminName == null || adminName.isBlank()
                || adminRepository.findByName(adminName) == null) {
            throw new SecurityException("A registered admin is required");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
