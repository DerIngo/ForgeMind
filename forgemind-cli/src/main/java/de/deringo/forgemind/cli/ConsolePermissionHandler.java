package de.deringo.forgemind.cli;

import de.deringo.forgemind.core.permission.PermissionHandler;
import de.deringo.forgemind.core.permission.PermissionRequest;

import java.util.Scanner;

public final class ConsolePermissionHandler
        implements PermissionHandler {

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public boolean requestPermission(
            PermissionRequest request
    ) {

        System.out.println();
        System.out.println("Permission required");
        System.out.println("Capability: " + request.capability());
        System.out.println("Tool: " + request.toolName());
        System.out.println("Arguments: " + request.arguments());
        System.out.print("Allow? [y/N]: ");

        String answer = scanner.nextLine();

        return answer.equalsIgnoreCase("y")
                || answer.equalsIgnoreCase("yes");
    }
}