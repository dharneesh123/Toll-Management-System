package com.toll.cli;

import com.toll.models.Lane;
import com.toll.models.TollTypes.LaneStatus;
import com.toll.models.TollTypes.LaneType;
import com.toll.services.LaneService;
import java.util.Scanner;

public final class LaneCLI {
    private final LaneService service;

    public LaneCLI(LaneService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nLane: 1) Create  2) Update status  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> create(scanner);
                    case "2" -> update(scanner);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void create(Scanner scanner) {
        Lane lane = service.createLane(
                CliSupport.read(scanner, "Admin name: "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.readInt(scanner, "Lane number: "),
                CliSupport.readEnum(scanner, "Lane type", LaneType.class),
                CliSupport.readEnum(scanner, "Lane status", LaneStatus.class));
        printLane(lane);
    }

    private void update(Scanner scanner) {
        Lane lane = service.updateLaneStatus(
                CliSupport.read(scanner, "Admin name: "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.read(scanner, "Lane ID: "),
                CliSupport.readEnum(scanner, "New status", LaneStatus.class));
        printLane(lane);
    }

    private static void printLane(Lane lane) {
        System.out.println("Lane " + lane.number() + " (ID: " + lane.id() + "): "
                + lane.type() + ", " + lane.status());
    }
}