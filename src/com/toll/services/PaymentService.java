package com.toll.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import com.toll.models.FASTagAccount;
import com.toll.models.Payment;
import com.toll.models.TollTypes.PaymentMethod;
import com.toll.models.TollTypes.PaymentStatus;
import com.toll.models.TollTypes.PaymentType;
import com.toll.models.TollTypes.AccountStatus;
import com.toll.repository.FASTagAccountRepository;
import com.toll.repository.PaymentRepository;

public class PaymentService {
    private static final BigDecimal MIN_RECHARGE = new BigDecimal("100");
    private static final BigDecimal MAX_RECHARGE = new BigDecimal("10000");
    private static final BigDecimal MAX_BALANCE = new BigDecimal("50000");

    private final PaymentRepository paymentRepository;
    private final FASTagAccountRepository accountRepository;

    public PaymentService(
            PaymentRepository paymentRepository, FASTagAccountRepository accountRepository) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
    }

    public Payment recharge(
            String tagId,
            BigDecimal amount,
            PaymentMethod method,
            PaymentStatus status,
            LocalDateTime time) {
        FASTagAccount account = findAccount(tagId);
        if (account.status() == AccountStatus.CLOSED) {
            throw new IllegalStateException("A closed FASTag account cannot be recharged");
        }
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(time, "time");
        if (amount.compareTo(MIN_RECHARGE) < 0 || amount.compareTo(MAX_RECHARGE) > 0) {
            throw new IllegalArgumentException("Recharge amount must be between Rs. 100 and Rs. 10,000");
        }
        if (method != PaymentMethod.UPI && method != PaymentMethod.CARD
                && method != PaymentMethod.NET_BANKING) {
            throw new IllegalArgumentException("Recharge method must be UPI, CARD, or NET_BANKING");
        }
        if (status == PaymentStatus.REFUNDED) {
            throw new IllegalArgumentException("Use refundRecharge to refund a paid recharge");
        }
        if (status == PaymentStatus.PAID
                && account.balance().add(amount).compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Balance cannot exceed Rs. 50,000");
        }

        Payment payment = new Payment(
                PaymentType.RECHARGE, tagId, amount, time, method, status, null);
        if (status == PaymentStatus.PAID) {
            account.credit(amount);
            accountRepository.save(account);
        }
        paymentRepository.save(payment);
        return payment;
    }

    public Payment recordTollPayment(
            String transactionId,
            BigDecimal amount,
            PaymentMethod method,
            LocalDateTime time,
            String laneOperator) {
        requireText(transactionId, "Transaction ID");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(time, "time");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Toll payment cannot be negative");
        }
        if (method != PaymentMethod.CASH && method != PaymentMethod.FASTAG) {
            throw new IllegalArgumentException("Toll payment method must be CASH or FASTAG");
        }
        if (method == PaymentMethod.CASH) {
            requireText(laneOperator, "Lane operator");
        }

        Payment payment = new Payment(
                PaymentType.CASH_TOLL, transactionId, amount, time, method, PaymentStatus.PAID,
                laneOperator);
        paymentRepository.save(payment);
        return payment;
    }

    public Payment recordPassPayment(
            String transactionId, BigDecimal amount, LocalDateTime time) {
        requireText(transactionId, "Transaction ID");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(time, "time");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Pass payment cannot be negative");
        }
        Payment payment = new Payment(
                PaymentType.PASS, transactionId, amount, time, PaymentMethod.FASTAG,
                PaymentStatus.PAID, null);
        paymentRepository.save(payment);
        return payment;
    }

    public Payment refundTollToAccount(
            String tagId, String transactionId, BigDecimal amount, LocalDateTime time) {
        FASTagAccount account = findAccount(tagId);
        requireText(transactionId, "Transaction ID");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(time, "time");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }
        if (account.balance().add(amount).compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Refund would exceed the Rs. 50,000 balance limit");
        }

        account.credit(amount);
        accountRepository.save(account);
        Payment payment = new Payment(
                PaymentType.REFUND, transactionId, amount, time, PaymentMethod.FASTAG,
                PaymentStatus.PAID, null);
        paymentRepository.save(payment);
        return payment;
    }

    public Payment refundRecharge(String paymentId, LocalDateTime time) {
        requireText(paymentId, "Payment ID");
        Objects.requireNonNull(time, "time");
        Payment original = paymentRepository.findById(paymentId);
        if (original == null || original.type() != PaymentType.RECHARGE) {
            throw new IllegalArgumentException("Paid recharge not found: " + paymentId);
        }
        if (original.status() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only a paid recharge can be refunded");
        }
        FASTagAccount account = findAccount(original.referenceId());
        if (account.balance().compareTo(original.amount()) < 0) {
            throw new IllegalStateException("Recharge cannot be refunded because the balance was spent");
        }

        account.debit(original.amount());
        accountRepository.save(account);
        Payment refunded = new Payment(
                original.id(), original.type(), original.referenceId(), original.amount(), time,
                original.method(), PaymentStatus.REFUNDED, original.laneOperator());
        paymentRepository.save(refunded);
        return refunded;
    }

    private FASTagAccount findAccount(String tagId) {
        requireText(tagId, "Tag ID");
        FASTagAccount account = accountRepository.findById(tagId);
        if (account == null) {
            throw new IllegalArgumentException("FASTag account not found: " + tagId);
        }
        return account;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
