package de.deringo.forgemind.cli;

import java.util.Scanner;

import de.deringo.forgemind.core.permission.PermissionGrant;
import de.deringo.forgemind.core.permission.PermissionHandler;
import de.deringo.forgemind.core.permission.PermissionRequest;
import de.deringo.forgemind.core.permission.PermissionScope;

public final class ConsolePermissionHandler
        implements PermissionHandler {

    private final Scanner scanner =
            new Scanner(System.in);

    @Override
    public PermissionGrant requestPermission(
            PermissionRequest request
    ) {

        System.out.println();
        System.out.println("Permission required");
        System.out.println();

        System.out.println(
                "Capability: "
                        + request.capability()
        );

        System.out.println(
                "Tool: "
                        + request.toolName()
        );

        System.out.println(
                "Resource: "
                        + request.key().resource()
        );

        System.out.println(
                "Arguments: "
                        + request.arguments()
        );

        System.out.println();
        System.out.println("[o] Allow once");
        System.out.println("[s] Allow for session");
        System.out.println("[d] Deny");
        System.out.println();

        System.out.print("Choice: ");

        String answer =
                scanner.nextLine()
                        .trim()
                        .toLowerCase();

        return switch (answer) {

            case "o" ->
                    PermissionGrant.allow(
                            PermissionScope.ONCE
                    );

            case "s" ->
                    PermissionGrant.allow(
                            PermissionScope.SESSION
                    );

            default ->
                    PermissionGrant.deny();
        };
    }
}