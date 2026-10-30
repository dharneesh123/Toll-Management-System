package com.toll.cli;

import com.toll.models.FASTagAccount;
import com.toll.services.FASTagAccountService;
import java.util.Scanner;

public final class FASTagCLI {
    private final FASTagAccountService service;

    public FASTagCLI(FASTagAccountService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (scanner.hasNextLine()) {
            System.out.println("\nFASTag: 1) Create  2) Find  3) Blacklist  4) Close  0) Back");
            String choice = CliSupport.read(scanner, "Choose: ");
            try {
                switch (choice) {
                    case "1" -> create(scanner);
                    case "2" -> printAccount(service.findAccount(
                            CliSupport.read(scanner, "Tag ID: ")));
                    case "3" -> printAccount(service.blacklistAccount(
                            CliSupport.read(scanner, "Admin name: "),
                            CliSupport.read(scanner, "Tag ID: ")));
                    case "4" -> printAccount(service.closeAccount(
                            CliSupport.read(scanner, "Admin name: "),
                            CliSupport.read(scanner, "Tag ID: ")));
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void create(Scanner scanner) {
        printAccount(service.createAccount(
                CliSupport.read(scanner, "Admin name: "),
                CliSupport.read(scanner, "Tag ID: "),
                CliSupport.read(scanner, "Vehicle registration: "),
                CliSupport.readAmount(scanner, "Opening balance: Rs. ")));
    }

    private static void printAccount(FASTagAccount account) {
        System.out.println("FASTag " + account.tagId() + " for " + account.vehicleRegistration()
                + ": Rs. " + account.balance() + ", " + account.status()
                + (account.blacklisted() ? ", BLACKLISTED" : ""));
    }
}