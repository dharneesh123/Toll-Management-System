package com.toll.cli;

import com.toll.models.TollRate;
import com.toll.models.TollTypes.TripType;
import com.toll.models.TollTypes.VehicleType;
import com.toll.services.TollRateService;
import java.util.Scanner;

public final class TollRateCLI {
    private final TollRateService service;

    public TollRateCLI(TollRateService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nToll rates: 1) Set rate  2) Find effective  3) Calculate  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> setRate(scanner);
                    case "2" -> System.out.println(service.findEffectiveRate(
                            CliSupport.read(scanner, "Plaza ID: "),
                            CliSupport.readEnum(scanner, "Vehicle type", VehicleType.class),
                            CliSupport.readEnum(scanner, "Trip type", TripType.class),
                            CliSupport.readDate(scanner, "Date (YYYY-MM-DD): ")));
                    case "3" -> System.out.println("Toll amount: Rs. " + service.calculateTollAmount(
                            CliSupport.read(scanner, "Plaza ID: "),
                            CliSupport.readEnum(scanner, "Vehicle type", VehicleType.class),
                            CliSupport.readEnum(scanner, "Trip type", TripType.class),
                            CliSupport.readDate(scanner, "Date (YYYY-MM-DD): ")));
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void setRate(Scanner scanner) {
        TollRate rate = service.setRate(
                CliSupport.read(scanner, "Admin name: "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.readEnum(scanner, "Vehicle type", VehicleType.class),
                CliSupport.readEnum(scanner, "Trip type", TripType.class),
                CliSupport.readDate(scanner, "Effective date (YYYY-MM-DD): "),
                CliSupport.readAmount(scanner, "Amount: Rs. "));
        System.out.println("Rate saved: " + rate);
    }
}