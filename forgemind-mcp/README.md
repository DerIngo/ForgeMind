# ForgeMind MCP – Hello World

Eigenständiger stdio-MCP-Server mit dem offiziellen Java-SDK 2.0.1.
Er benötigt weder ein LLM noch eine Datenbank oder forgemind-core.

## Bauen

Im Repository-Stamm (JDK 25):

```powershell
.\mvnw.cmd -pl forgemind-mcp -am package
```

Unter Linux: `./mvnw -pl forgemind-mcp -am package`.

## Starten

```powershell
java -jar forgemind-mcp/target/forgemind-mcp-0.1.0-SNAPSHOT-all.jar
```

Der Prozess wartet auf MCP-Nachrichten über stdin. Er ist keine interaktive
Textkonsole und druckt beim Start keine Begrüßung. stdout gehört ausschließlich
dem JSON-RPC-Protokoll; eigene Diagnoseausgaben müssen nach stderr.
Ein MCP-Client startet diesen Prozess und beendet die Verbindung durch Schließen
von stdin. Es ist kein Netzwerk-Port erforderlich.

Für einen stdio-fähigen MCP-Client:

- Command: `java` (oder absoluter Pfad zur Java-25-Executable)
- Arguments: `-jar`, absoluter Pfad zur erzeugten `*-all.jar`
- Transport: stdio

Die konkrete Konfigurationsdatei hängt vom verwendeten Client ab.
ForgeMinds Agent ist noch nicht als MCP-Client angebunden.

## Tool

`hello` erwartet genau einen erforderlichen String-Parameter `name` mit mindestens
einem Zeichen. Zusätzliche Parameter sind nicht erlaubt.

```json
{"name":"World"}
```

Ergebnis als MCP-TextContent: `Hello, World!`.

## Ablauf zum Lernen

Der Client sendet jede JSON-Nachricht als einzelne Zeile und wartet bei Requests
auf die Antwort, bevor er mit dem nächsten Schritt fortfährt:

1. `initialize`: Protokollversion und Fähigkeiten aushandeln.
2. `notifications/initialized`: abgeschlossene Initialisierung bestätigen.
3. `tools/list`: Beschreibung und Eingabeschema von `hello` abfragen.
4. `tools/call`: das Tool aufrufen.

Beispiel für Schritt 4:

```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"hello","arguments":{"name":"World"}}}
```

`ForgeMindMCPTest` führt den Ablauf mit einem echten Java-Unterprozess aus,
prüft außerdem ungültige Parameter und das Beenden bei EOF. Alle stdout-Antworten
werden als JSON gelesen, sodass gewöhnliche Konsolenausgaben den Test stören würden.

Die fertige JAR lässt sich mit demselben Test prüfen (Pfad gegebenenfalls anpassen):

```powershell
.\mvnw.cmd -pl forgemind-mcp '-Dmcp.test.jar=C:/dev/workspace/ForgeMind/forgemind-mcp/target/forgemind-mcp-0.1.0-SNAPSHOT-all.jar' test
```

SDK-Dokumentation: https://java.sdk.modelcontextprotocol.io/latest/server/
