package com.toll.cli;

import com.toll.models.Payment;
import com.toll.models.TollTypes.PaymentMethod;
import com.toll.models.TollTypes.PaymentStatus;
import com.toll.services.PaymentService;
import java.util.Scanner;

public final class RechargeCLI {
    private final PaymentService service;

    public RechargeCLI(PaymentService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nRecharge: 1) Recharge FASTag  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                if (choice.equals("0")) return;
                if (choice.equals("1")) recharge(scanner);
                else System.out.println("Invalid choice.");
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void recharge(Scanner scanner) {
        Payment payment = service.recharge(
                CliSupport.read(scanner, "Tag ID: "),
                CliSupport.readAmount(scanner, "Amount: Rs. "),
                CliSupport.readEnum(scanner, "Payment method", PaymentMethod.class,
                    PaymentMethod.UPI, PaymentMethod.CARD, PaymentMethod.NET_BANKING),
                CliSupport.readEnum(scanner, "Payment status", PaymentStatus.class,
                    PaymentStatus.PAID, PaymentStatus.FAILED),
                CliSupport.readDateTime(scanner, "Payment time (YYYY-MM-DDTHH:MM): "));
        System.out.println("Recharge recorded: " + payment);
    }
}