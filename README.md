# ForgeMind
A modular Java-based AI agent with pluggable LLMs, tools, MCP integrations and RAG.

The project currently contains a minimal executable skeleton. AI functionality is
not implemented yet; the CLI prints `Hello World!` using the core module.

## Requirements

- JDK 25 or newer; compilation targets Java 25.
- `JAVA_HOME` must point to the JDK used for the build. The Maven Enforcer plugin
  checks this during `validate` and reports unsupported Java/Maven versions early.
- Use the committed Maven Wrapper (Maven 3.9.16). A separate Maven installation
  is not required. The first build needs internet access to download Maven and dependencies.
- On Linux/macOS, the wrapper needs `curl` or `wget`, and `unzip`.

Check the JDK actually used by Maven; it can differ from `java -version`:

```sh
./mvnw --version
```

For example, with OpenJDK 25 installed in WSL/Linux:

```sh
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

On Windows PowerShell, set `$env:JAVA_HOME` to your JDK directory and use
`.\mvnw.cmd` instead of `./mvnw` for the commands below.

## Modules

| Module | Responsibility | Java package |
| --- | --- | --- |
| `forgemind-core` | Application logic, independent of the CLI | `de.deringo.forgemind.core` |
| `forgemind-cli` | Command-line entry point; depends on core | `de.deringo.forgemind.cli` |

The root POM aggregates both modules and provides shared dependency and build
configuration. Modules share the project version. These are Maven modules;
the project does not currently use Java's module system (`module-info.java`).

## Build and test

Run from the repository root:

```sh
./mvnw clean verify
```

To build the CLI and its required modules explicitly:

```sh
./mvnw -pl forgemind-cli -am clean verify
```

The CLI test starts a separate JVM and checks the actual entry point, successful
termination, greeting supplied by core, and empty error output. Add behavior tests
in the corresponding module as application functionality is implemented.

## Run

After the build, run the executable JAR with JDK 25 or newer:

```sh
java -jar forgemind-cli/target/forgemind-cli-0.1.0-SNAPSHOT-all.jar
```

Expected output: `Hello World!`

The `-all.jar` contains the CLI and its runtime dependencies, including core.
The regular module JARs are also produced. After changing the project version,
adjust the filename in the start command accordingly.

## Continuous integration

GitHub Actions runs the wrapper build with JDK 25 on pushes and pull requests,
then starts the packaged executable JAR and checks its output.
