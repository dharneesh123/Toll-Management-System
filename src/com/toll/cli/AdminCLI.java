package com.toll.cli;

import com.toll.services.AdminService;
import java.util.Scanner;

public final class AdminCLI {
    private final AdminService service;

    public AdminCLI(AdminService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (scanner.hasNextLine()) {
            System.out.println("\nAdmin: 1) Register  2) Find  0) Back");
            String choice = CliSupport.read(scanner, "Choose: ");
            try {
                switch (choice) {
                    case "1" -> System.out.println("Registered: "
                            + service.registerAdmin(CliSupport.read(scanner, "Admin name: ")));
                    case "2" -> System.out.println("Found: "
                            + service.findAdmin(CliSupport.read(scanner, "Admin name: ")));
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }
}