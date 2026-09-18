package de.deringo.forgemind.core.tool.filesystem;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

import java.util.Map;

public final class ReplaceTextTool implements AgentTool {

    private final ProjectWorkspace workspace;

    public ReplaceTextTool(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "replace_text",
                """
                Replaces exactly one occurrence of text in an existing project file.
                Read the file first and use the exact existing text.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Path relative to the project root."
                                ),
                                "old_text", Map.of(
                                        "type", "string",
                                        "description",
                                        "Exact text currently present in the file."
                                ),
                                "new_text", Map.of(
                                        "type", "string",
                                        "description",
                                        "Replacement text."
                                )
                        ),
                        "required", new String[]{
                                "path",
                                "old_text",
                                "new_text"
                        }
                )
        );
    }

    @Override
    public Capability capability() {
        return Capability.WRITE;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {

        String path = (String) arguments.get("path");
        String oldText = (String) arguments.get("old_text");
        String newText = (String) arguments.get("new_text");

        workspace.replaceText(path, oldText, newText);

        return new ToolResult(
                "Successfully updated file: " + path
        );
    }
}