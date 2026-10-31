package com.toll.cli;

import com.toll.models.Dispute;
import com.toll.services.DisputeService;
import java.util.Scanner;

public final class DisputeCLI {
    private final DisputeService service;

    public DisputeCLI(DisputeService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nDisputes: 1) Submit  2) Resolve  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> submit(scanner);
                    case "2" -> resolve(scanner);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void submit(Scanner scanner) {
        Dispute dispute = service.submitDispute(
                CliSupport.read(scanner, "Vehicle registration: "),
                CliSupport.read(scanner, "Transaction ID: "),
                CliSupport.read(scanner, "Reason: "),
                CliSupport.readDateTime(scanner, "Submitted at (YYYY-MM-DDTHH:MM): "));
        System.out.println("Dispute submitted: " + dispute.id());
    }

    private void resolve(Scanner scanner) {
        Dispute dispute = service.resolveDispute(
                CliSupport.read(scanner, "Admin name: "),
                CliSupport.read(scanner, "Dispute ID: "),
                CliSupport.readYesNo(scanner, "Approve refund"),
                CliSupport.readDateTime(scanner, "Resolved at (YYYY-MM-DDTHH:MM): "));
        System.out.println("Dispute resolved. Refunded: " + dispute.refunded());
    }
}