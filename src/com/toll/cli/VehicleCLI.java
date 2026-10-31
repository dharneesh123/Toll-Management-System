package com.toll.cli;

import com.toll.models.TollTypes.VehicleType;
import com.toll.models.Vehicle;
import com.toll.services.VehicleService;
import java.util.Scanner;

public final class VehicleCLI {
    private final VehicleService service;

    public VehicleCLI(VehicleService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nVehicles: 1) Register  2) Find  3) List all  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> register(scanner);
                    case "2" -> System.out.println(service.findByRegistration(
                            CliSupport.read(scanner, "Registration: ")));
                    case "3" -> service.findAllVehicles().forEach(System.out::println);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void register(Scanner scanner) {
        Vehicle vehicle = new Vehicle(
                CliSupport.read(scanner, "Registration: "),
                CliSupport.readEnum(scanner, "Vehicle type", VehicleType.class),
                CliSupport.read(scanner, "Owner name: "),
                CliSupport.read(scanner, "Phone: "),
                CliSupport.readYesNo(scanner, "Emergency vehicle"));
        System.out.println("Registered: " + service.registerVehicle(vehicle));
    }
}