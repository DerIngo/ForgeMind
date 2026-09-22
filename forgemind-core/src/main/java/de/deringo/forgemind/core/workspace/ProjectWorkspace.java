package de.deringo.forgemind.core.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class ProjectWorkspace {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            ".idea",
            ".vscode",
            "target",
            "build",
            "dist",
            "node_modules"
    );

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java",
            "xml",
            "properties",
            "yml",
            "yaml",
            "json",
            "md",
            "txt",
            "js",
            "jsx",
            "ts",
            "tsx",
            "css",
            "html",
            "sql"
    );

    private final Path projectRoot;

    public ProjectWorkspace(Path projectRoot) {
        this.projectRoot = projectRoot
                .toAbsolutePath()
                .normalize();
    }

    public Path root() {
        return projectRoot;
    }

    public Path resolve(String path) {

        Path resolved = projectRoot
                .resolve(path)
                .normalize();

        if (!resolved.startsWith(projectRoot)) {
            throw new IllegalArgumentException(
                    "Access outside project root is not allowed: " + path
            );
        }

        return resolved;
    }

    public Path resolveExistingFile(String path) {
        Path resolved = resolve(path);
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("Not an existing file: " + path);
        }
        try {
            Path realPath = resolved.toRealPath();
            if (!realPath.startsWith(projectRoot.toRealPath())) {
                throw new IllegalArgumentException("Access outside project root is not allowed: " + path);
            }
            return realPath;
        } catch (IOException e) {
            throw new IllegalStateException("Could not resolve file: " + path, e);
        }
    }

    public void writeFile(String path, String content) {
        Path resolved = resolveExistingFile(path);
        try {
            Files.writeString(resolved, content,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write file: " + path, e);
        }
    }

    public String readFile(String path) {

        Path resolved = resolve(path);

        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException(
                    "Not a file: " + path
            );
        }

        try {
            return Files.readString(resolved);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read file: " + path,
                    e
            );
        }
    }

    public List<Path> list(String path) {

        Path directory = resolve(path);

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "Not a directory: " + path
            );
        }

        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(this::isIncluded)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not list directory: " + path,
                    e
            );
        }
    }

    public Stream<Path> walk() throws IOException {
        return Files.walk(projectRoot)
                .filter(this::isIncluded);
    }

    public Path relative(Path path) {
        return projectRoot.relativize(path);
    }

    public String relativeString(Path path) {
        return relative(path)
                .toString()
                .replace('\\', '/');
    }
    
    private boolean isIncluded(Path path) {

        Path relative = projectRoot.relativize(path);

        for (Path part : relative) {
            if (IGNORED_DIRECTORIES.contains(part.toString())) {
                return false;
            }
        }

        return true;
    }
    
    public boolean isTextFile(Path path) {

        String filename = path.getFileName()
                .toString();

        int dot = filename.lastIndexOf('.');

        if (dot < 0) {
            return false;
        }

        String extension = filename
                .substring(dot + 1)
                .toLowerCase();

        return TEXT_EXTENSIONS.contains(extension);
    }
    
    public void replaceText(
            String path,
            String oldText,
            String newText
    ) {

        Path resolved = resolve(path);

        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException(
                    "Not a file: " + path
            );
        }

        try {
            String content = Files.readString(resolved);

            int first = content.indexOf(oldText);

            if (first < 0) {
                throw new IllegalArgumentException(
                        "Text to replace was not found in file: " + path
                );
            }

            int second = content.indexOf(
                    oldText,
                    first + oldText.length()
            );

            if (second >= 0) {
                throw new IllegalArgumentException(
                        "Text to replace occurs more than once in file: " + path
                );
            }

            String updated =
                    content.substring(0, first)
                            + newText
                            + content.substring(first + oldText.length());

            Files.writeString(resolved, updated);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not update file: " + path,
                    e
            );
        }
    }

    public void createFile(
            String path,
            String content
    ) {
        Path resolved = resolve(path);

        if (Files.exists(resolved)) {
            throw new IllegalArgumentException(
                    "File already exists: " + path
            );
        }

        try {
            Path parent = resolved.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    resolved,
                    content,
                    StandardOpenOption.CREATE_NEW
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create file: " + path,
                    e
            );
        }
    }
}
