package com.toll.cli;

import com.toll.models.Lane;
import com.toll.models.TollPlaza;
import com.toll.models.TollTypes.LaneStatus;
import com.toll.models.TollTypes.LaneType;
import com.toll.services.TollPlazaService;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class PlazaCLI {
    private final TollPlazaService service;

    public PlazaCLI(TollPlazaService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\nToll plaza: 1) Create  2) Open  0) Back");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> create(scanner);
                    case "2" -> open(scanner);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void create(Scanner scanner) {
        String admin = CliSupport.read(scanner, "Admin name: ");
        String name = CliSupport.read(scanner, "Plaza name: ");
        String highway = CliSupport.read(scanner, "Highway: ");
        String location = CliSupport.read(scanner, "Location: ");
        int laneCount = CliSupport.readInt(scanner, "Number of lanes (at least 2): ");
        List<Lane> lanes = new ArrayList<>();
        for (int laneNumber = 1; laneNumber <= laneCount; laneNumber++) {
            System.out.println("Lane " + laneNumber);
            LaneType type = CliSupport.readEnum(scanner, "Type", LaneType.class);
            LaneStatus status = CliSupport.readEnum(scanner, "Status", LaneStatus.class);
            lanes.add(new Lane(laneNumber, type, status));
        }
        TollPlaza plaza = service.createPlaza(admin, name, highway, location, lanes);
        System.out.println("Created plaza " + plaza.name() + " (ID: " + plaza.id() + ").");
    }

    private void open(Scanner scanner) {
        TollPlaza plaza = service.openPlaza(
                CliSupport.read(scanner, "Admin name: "),
                CliSupport.read(scanner, "Plaza ID: "),
                CliSupport.readDate(scanner, "Effective date (YYYY-MM-DD): "));
        System.out.println("Plaza " + plaza.name() + " is now open.");
    }
}