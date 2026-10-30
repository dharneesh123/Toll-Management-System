package com.toll.cli;

import com.toll.models.MonthlyPass;
import com.toll.services.MonthlyPassService;
import java.util.Scanner;

public final class PassCLI {
    private final MonthlyPassService service;

    public PassCLI(MonthlyPassService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (scanner.hasNextLine()) {
            System.out.println("\nMonthly pass: 1) Purchase  2) Check valid pass  0) Back");
            String choice = CliSupport.read(scanner, "Choose: ");
            try {
                switch (choice) {
                    case "1" -> purchase(scanner);
                    case "2" -> findValid(scanner);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void purchase(Scanner scanner) {
        MonthlyPass pass = service.purchasePass(
                CliSupport.read(scanner, "Vehicle registration: "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.readDate(scanner, "Start date (YYYY-MM-DD): "),
                CliSupport.readDate(scanner, "End date (YYYY-MM-DD): "),
                CliSupport.readAmount(scanner, "Fee: Rs. "),
                CliSupport.readDate(scanner, "Purchase date (YYYY-MM-DD): "));
        System.out.println("Pass purchased: " + pass);
    }

    private void findValid(Scanner scanner) {
        MonthlyPass pass = service.findValidPass(
                CliSupport.read(scanner, "Vehicle registration: "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.readDate(scanner, "Date (YYYY-MM-DD): "));
        System.out.println(pass == null ? "No valid pass found." : pass);
    }
}