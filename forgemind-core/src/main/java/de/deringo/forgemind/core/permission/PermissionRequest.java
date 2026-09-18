package de.deringo.forgemind.core.permission;

import java.util.Map;

public record PermissionRequest(
        Capability capability,
        String toolName,
        Map<String, Object> arguments,
        PermissionKey key
) {
}