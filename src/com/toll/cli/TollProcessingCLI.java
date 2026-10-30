package com.toll.cli;

import com.toll.models.CrossingResult;
import com.toll.models.TollTransaction;
import com.toll.models.TollTypes.VehicleType;
import com.toll.services.TollProcessingService;
import java.util.Scanner;

public final class TollProcessingCLI {
    private final TollProcessingService service;

    public TollProcessingCLI(TollProcessingService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (scanner.hasNextLine()) {
            System.out.println("\nToll processing: 1) Process crossing  0) Back");
            String choice = CliSupport.read(scanner, "Choose: ");
            try {
                if (choice.equals("0")) return;
                if (choice.equals("1")) process(scanner);
                else System.out.println("Invalid choice.");
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void process(Scanner scanner) {
        CrossingResult result = service.processCrossing(
                CliSupport.read(scanner, "Vehicle registration: "),
                CliSupport.read(scanner, "FASTag ID (blank if none): "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.read(scanner, "FASTag lane ID: "),
                CliSupport.read(scanner, "Cash lane ID: "),
                CliSupport.read(scanner, "Emergency lane ID: "),
                CliSupport.readEnum(scanner, "Observed vehicle type", VehicleType.class),
                CliSupport.readDateTime(scanner, "Crossing time (YYYY-MM-DDTHH:MM): "),
                CliSupport.read(scanner, "Lane operator (blank if not cash): "));
        System.out.println("Barrier opened: " + result.barrierOpened()
                + ", sent to cash: " + result.sentToCash());
        for (TollTransaction transaction : result.transactions()) {
            System.out.println(transaction);
        }
    }
}