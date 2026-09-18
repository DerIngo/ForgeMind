package de.deringo.forgemind.core.permission;

public record PermissionKey(
        Capability capability,
        String toolName,
        String resource
) {
}