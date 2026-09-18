package de.deringo.forgemind.core.tool.command;

import java.util.List;
import java.util.Map;

import de.deringo.forgemind.core.command.CommandExecutor;
import de.deringo.forgemind.core.command.CommandResult;
import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.permission.PermissionKey;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;

public final class RunCommandTool implements AgentTool {

    private final CommandExecutor commandExecutor;

    public RunCommandTool(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PermissionKey permissionKey(
            Map<String, Object> arguments
    ) {

        List<String> command =
                (List<String>) arguments.get("command");

        String executable =
                command == null || command.isEmpty()
                        ? ""
                        : command.getFirst();

        return new PermissionKey(
                Capability.EXECUTE,
                definition().name(),
                executable
        );
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "run_command",
                """
                Executes a command inside the project workspace.
                Use this for builds, tests, compilers and other
                development commands.
                """,
                Map.of(
                        "type", "object",

                        "properties", Map.of(
                                "command", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "string"
                                        ),
                                        "description",
                                        "Command and arguments as an array."
                                ),

                                "working_directory", Map.of(
                                        "type", "string",
                                        "description",
                                        "Working directory relative to the project root. Use '.' for the project root."
                                )
                        ),

                        "required", new String[]{
                                "command",
                                "working_directory"
                        }
                )
        );
    }

    @Override
    public Capability capability() {
        return Capability.EXECUTE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> arguments) {

        List<String> command =
                (List<String>) arguments.get("command");

        String workingDirectory =
                (String) arguments.get("working_directory");

        CommandResult result =
                commandExecutor.execute(
                        command,
                        workingDirectory
                );

        return new ToolResult(
                """
                Exit code: %d

                Output:
                %s
                """.formatted(
                        result.exitCode(),
                        result.output()
                )
        );
    }
}