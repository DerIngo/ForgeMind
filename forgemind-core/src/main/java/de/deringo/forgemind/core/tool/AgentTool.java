package de.deringo.forgemind.core.tool;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.permission.PermissionKey;

import java.util.Map;

public interface AgentTool {

    ToolDefinition definition();

    Capability capability();

    /** Validates arguments without side effects, before permission evaluation. */
    default void validateArguments(Map<String, Object> arguments) {
    }

    default PermissionKey permissionKey(
            Map<String, Object> arguments
    ) {
        return new PermissionKey(
                capability(),
                definition().name(),
                definition().name()
        );
    }

    ToolResult execute(
            Map<String, Object> arguments
    );
}
