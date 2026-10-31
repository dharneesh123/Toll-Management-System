package com.toll;

import java.util.Scanner;
import com.toll.cli.AdminCLI;
import com.toll.cli.DisputeCLI;
import com.toll.cli.FASTagCLI;
import com.toll.cli.LaneCLI;
import com.toll.cli.PassCLI;
import com.toll.cli.PaymentCLI;
import com.toll.cli.PlazaCLI;
import com.toll.cli.RechargeCLI;
import com.toll.cli.ReportCLI;
import com.toll.cli.TollProcessingCLI;
import com.toll.cli.TollRateCLI;
import com.toll.cli.VehicleCLI;
import com.toll.repository.AdminRepository;
import com.toll.repository.DisputeRepository;
import com.toll.repository.FASTagAccountRepository;
import com.toll.repository.MonthlyPassRepository;
import com.toll.repository.PaymentRepository;
import com.toll.repository.TollPlazaRepository;
import com.toll.repository.TollRateRepository;
import com.toll.repository.TollTransactionRepository;
import com.toll.repository.VehicleRepository;
import com.toll.services.AdminService;
import com.toll.services.DisputeService;
import com.toll.services.FASTagAccountService;
import com.toll.services.LaneService;
import com.toll.services.MonthlyPassService;
import com.toll.services.PaymentService;
import com.toll.services.ReportService;
import com.toll.services.TollPlazaService;
import com.toll.services.TollProcessingService;
import com.toll.services.TollRateService;
import com.toll.services.VehicleService;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        AdminRepository adminRepository = new AdminRepository();
        DisputeRepository disputeRepository = new DisputeRepository();
        FASTagAccountRepository accountRepository = new FASTagAccountRepository();
        MonthlyPassRepository passRepository = new MonthlyPassRepository();
        PaymentRepository paymentRepository = new PaymentRepository();
        TollPlazaRepository plazaRepository = new TollPlazaRepository();
        TollRateRepository rateRepository = new TollRateRepository();
        TollTransactionRepository transactionRepository = new TollTransactionRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();

        AdminService adminService = new AdminService(adminRepository);
        TollPlazaService plazaService = new TollPlazaService(
                plazaRepository, rateRepository, adminRepository);
        LaneService laneService = new LaneService(plazaRepository, adminRepository);
        TollRateService rateService = new TollRateService(
                rateRepository, plazaRepository, adminRepository);
        VehicleService vehicleService = new VehicleService(vehicleRepository);
        FASTagAccountService accountService = new FASTagAccountService(
                accountRepository, vehicleRepository, adminRepository);
        PaymentService paymentService = new PaymentService(paymentRepository, accountRepository);
        MonthlyPassService passService = new MonthlyPassService(
                passRepository, plazaRepository, vehicleRepository);
        TollProcessingService tollProcessingService = new TollProcessingService(
                plazaRepository,
                vehicleRepository,
                accountRepository,
                transactionRepository,
                rateService,
                passService,
                paymentService);
        DisputeService disputeService = new DisputeService(
                disputeRepository,
                transactionRepository,
                accountRepository,
                adminRepository,
                paymentService);
        ReportService reportService = new ReportService(transactionRepository, accountRepository);

        AdminCLI adminCLI = new AdminCLI(adminService);
        PlazaCLI plazaCLI = new PlazaCLI(plazaService);
        LaneCLI laneCLI = new LaneCLI(laneService);
        TollRateCLI rateCLI = new TollRateCLI(rateService);
        VehicleCLI vehicleCLI = new VehicleCLI(vehicleService);
        FASTagCLI fastagCLI = new FASTagCLI(accountService);
        TollProcessingCLI processingCLI = new TollProcessingCLI(tollProcessingService);
        RechargeCLI rechargeCLI = new RechargeCLI(paymentService);
        PassCLI passCLI = new PassCLI(passService);
        PaymentCLI paymentCLI = new PaymentCLI(paymentService);
        DisputeCLI disputeCLI = new DisputeCLI(disputeService);
        ReportCLI reportCLI = new ReportCLI(reportService);

        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(scanner, adminCLI, plazaCLI, laneCLI, rateCLI, vehicleCLI, fastagCLI,
                    processingCLI, rechargeCLI, passCLI, paymentCLI, disputeCLI, reportCLI);
        }
    }

    private static void runMenu(
            Scanner scanner,
            AdminCLI adminCLI,
            PlazaCLI plazaCLI,
            LaneCLI laneCLI,
            TollRateCLI rateCLI,
            VehicleCLI vehicleCLI,
            FASTagCLI fastagCLI,
            TollProcessingCLI processingCLI,
            RechargeCLI rechargeCLI,
            PassCLI passCLI,
            PaymentCLI paymentCLI,
            DisputeCLI disputeCLI,
            ReportCLI reportCLI) {
        while (scanner.hasNextLine()) {
            System.out.println("\n=== Toll Management System ===");
            System.out.println("1) Admins | 2) Toll plazas | 3) Lanes | 4) Toll rates | "
                    + "5) Vehicles | 6) FASTag accounts | 7) Process crossing | "
                    + "8) Recharge FASTag | 9) Monthly passes | 10) Payments | "
                    + "11) Disputes | 12) Reports | 0) Exit");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> adminCLI.run(scanner);
                case "2" -> plazaCLI.run(scanner);
                case "3" -> laneCLI.run(scanner);
                case "4" -> rateCLI.run(scanner);
                case "5" -> vehicleCLI.run(scanner);
                case "6" -> fastagCLI.run(scanner);
                case "7" -> processingCLI.run(scanner);
                case "8" -> rechargeCLI.run(scanner);
                case "9" -> passCLI.run(scanner);
                case "10" -> paymentCLI.run(scanner);
                case "11" -> disputeCLI.run(scanner);
                case "12" -> reportCLI.run(scanner);
                case "0" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Invalid choice. Choose 0 through 12.");
            }
        }
    }
}
