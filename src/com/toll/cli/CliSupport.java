package com.toll.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Scanner;

final class CliSupport {
    private CliSupport() { }

    static String read(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    static int readInt(Scanner scanner, String prompt) {
        return Integer.parseInt(read(scanner, prompt));
    }

    static BigDecimal readAmount(Scanner scanner, String prompt) {
        return new BigDecimal(read(scanner, prompt));
    }

    static LocalDate readDate(Scanner scanner, String prompt) {
        return LocalDate.parse(read(scanner, prompt));
    }

    static LocalDateTime readDateTime(Scanner scanner, String prompt) {
        return LocalDateTime.parse(read(scanner, prompt));
    }

    static boolean readYesNo(Scanner scanner, String prompt) {
        while (true) {
            String answer = read(scanner, prompt + " (y/n): ");
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) return true;
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) return false;
            System.out.println("Enter y or n.");
        }
    }

    static <E extends Enum<E>> E readEnum(Scanner scanner, String prompt, Class<E> enumType) {
        return readEnum(scanner, prompt, enumType, enumType.getEnumConstants());
    }

    @SafeVarargs
    static <E extends Enum<E>> E readEnum(
            Scanner scanner, String prompt, Class<E> enumType, E... allowedValues) {
        String[] allowedNames = new String[allowedValues.length];
        for (int index = 0; index < allowedValues.length; index++) {
            allowedNames[index] = allowedValues[index].name();
        }
        while (true) {
            String value = read(scanner, prompt + " " + String.join(", ", allowedNames) + ": ");
            try {
                E selected = Enum.valueOf(enumType, value.toUpperCase());
                for (E allowedValue : allowedValues) {
                    if (selected == allowedValue) return selected;
                }
            } catch (IllegalArgumentException exception) {
            }
            System.out.println("Invalid choice. Choose one of: " + String.join(", ", allowedNames));
        }
    }

    private static <E extends Enum<E>> String[] enumNames(Class<E> enumType) {
        E[] values = enumType.getEnumConstants();
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; index++) names[index] = values[index].name();
        return names;
    }

    static void reportError(RuntimeException exception) {
        String message = exception.getMessage();
        System.out.println("Error: " + (message == null ? exception.getClass().getSimpleName() : message));
    }
}