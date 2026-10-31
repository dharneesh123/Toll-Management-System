package com.toll.cli;

import com.toll.models.Payment;
import com.toll.models.TollTypes.PaymentMethod;
import com.toll.services.PaymentService;
import java.util.Scanner;

public final class PaymentCLI {
    private final PaymentService service;

    public PaymentCLI(PaymentService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nPayments: 1) Record toll payment  2) Record pass payment"
                    + "  3) Refund toll to FASTag  4) Refund recharge  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> recordToll(scanner);
                    case "2" -> recordPass(scanner);
                    case "3" -> refundToll(scanner);
                    case "4" -> refundRecharge(scanner);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void recordToll(Scanner scanner) {
        Payment payment = service.recordTollPayment(
                CliSupport.read(scanner, "Transaction ID: "),
                CliSupport.readAmount(scanner, "Amount: Rs. "),
                CliSupport.readEnum(scanner, "Payment method", PaymentMethod.class,
                    PaymentMethod.CASH, PaymentMethod.FASTAG),
                CliSupport.readDateTime(scanner, "Payment time (YYYY-MM-DDTHH:MM): "),
                CliSupport.read(scanner, "Lane operator (required for CASH): "));
        System.out.println("Payment recorded: " + payment);
    }

    private void recordPass(Scanner scanner) {
        Payment payment = service.recordPassPayment(
                CliSupport.read(scanner, "Transaction ID: "),
                CliSupport.readAmount(scanner, "Amount: Rs. "),
                CliSupport.readDateTime(scanner, "Payment time (YYYY-MM-DDTHH:MM): "));
        System.out.println("Payment recorded: " + payment);
    }

    private void refundToll(Scanner scanner) {
        Payment payment = service.refundTollToAccount(
                CliSupport.read(scanner, "Tag ID: "),
                CliSupport.read(scanner, "Transaction ID: "),
                CliSupport.readAmount(scanner, "Refund amount: Rs. "),
                CliSupport.readDateTime(scanner, "Refund time (YYYY-MM-DDTHH:MM): "));
        System.out.println("Refund recorded: " + payment);
    }

    private void refundRecharge(Scanner scanner) {
        Payment payment = service.refundRecharge(
                CliSupport.read(scanner, "Recharge payment ID: "),
                CliSupport.readDateTime(scanner, "Refund time (YYYY-MM-DDTHH:MM): "));
        System.out.println("Recharge refunded: " + payment);
    }
}