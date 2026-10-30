package com.toll.cli;

import com.toll.models.FASTagAccount;
import com.toll.models.TollTransaction;
import com.toll.services.ReportService;
import java.time.LocalDate;
import java.util.Scanner;

public final class ReportCLI {
    private final ReportService service;

    public ReportCLI(ReportService service) {
        this.service = service;
    }

    public void run(Scanner scanner) {
        while (scanner.hasNextLine()) {
            System.out.println("\nReports: 1) Daily revenue  2) Lane vehicle count"
                    + "  3) Transaction summary  4) Failed transactions"
                    + "  5) Low-balance FASTags  0) Back");
            String choice = CliSupport.read(scanner, "Choose: ");
            try {
                switch (choice) {
                    case "1" -> showDailyRevenue(scanner);
                    case "2" -> showLaneCounts(scanner);
                    case "3" -> showTransactionSummary(scanner);
                    case "4" -> showFailedTransactions(scanner);
                    case "5" -> service.lowBalanceAccounts().forEach(ReportCLI::printAccount);
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException exception) {
                CliSupport.reportError(exception);
            }
        }
    }

    private void showDailyRevenue(Scanner scanner) {
        LocalDate date = CliSupport.readDate(scanner, "Date (YYYY-MM-DD): ");
        service.dailyRevenueByPlaza(date).forEach(
                (plazaId, amount) -> System.out.println(plazaId + ": Rs. " + amount));
    }

    private void showLaneCounts(Scanner scanner) {
        LocalDate date = CliSupport.readDate(scanner, "Date (YYYY-MM-DD): ");
        service.vehicleCountByLane(date).forEach(
                (laneId, count) -> System.out.println(laneId + ": " + count));
    }

    private void showTransactionSummary(Scanner scanner) {
        LocalDate date = CliSupport.readDate(scanner, "Date (YYYY-MM-DD): ");
        service.transactionSummary(date).forEach(
                (status, count) -> System.out.println(status + ": " + count));
    }

    private void showFailedTransactions(Scanner scanner) {
        LocalDate from = CliSupport.readDate(scanner, "From date (YYYY-MM-DD): ");
        LocalDate through = CliSupport.readDate(scanner, "Through date (YYYY-MM-DD): ");
        for (TollTransaction transaction : service.failedTransactions(from, through)) {
            System.out.println(transaction);
        }
    }

    private static void printAccount(FASTagAccount account) {
        System.out.println(account.tagId() + " / " + account.vehicleRegistration()
                + ": Rs. " + account.balance() + " (" + account.status() + ")");
    }
}