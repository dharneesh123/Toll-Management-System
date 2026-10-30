package com.toll.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toll.models.Payment;

public final class PaymentRepository {
    private final Map<String, Payment> payments = new LinkedHashMap<>();

    public void save(Payment payment) {
        payments.put(payment.id(), payment);
    }

    public Payment findById(String id) {
        return payments.get(id);
    }

    public List<Payment> findAll() {
        return List.copyOf(payments.values());
    }
}
