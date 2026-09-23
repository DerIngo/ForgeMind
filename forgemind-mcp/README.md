# ForgeMind MCP – Hello World über HTTP

Eigenständiger Streamable-HTTP-MCP-Server mit dem offiziellen Java-SDK 2.0.1
und eingebettetem Tomcat 11.0.26. Kein LLM und keine Datenbank erforderlich.
Der vorherige stdio-Start wurde durch HTTP ersetzt.

## Bauen

Im Repository-Stamm mit JDK 25:

```powershell
.\mvnw.cmd -pl forgemind-mcp -am package
```

Unter Linux: `./mvnw -pl forgemind-mcp -am package`.

## 1. Server starten

In einem eigenen Terminal:

```powershell
java -jar forgemind-mcp/target/forgemind-mcp-0.1.0-SNAPSHOT-all.jar
```

In der IDE: `de.deringo.forgemind.mcp.ForgeMindMCP` als Java-Anwendung starten.
Standard-Endpunkt: **http://127.0.0.1:8080/mcp**.
Der Server läuft unabhängig von Clients, bis du ihn mit Strg+C beendest.
Das Schließen eines Clients beendet nur dessen MCP-Sitzung.

Optionale Argumente: `[port] [bind-address]`, zum Beispiel `8081 127.0.0.1`.
Port `0` wählt einen freien Port, der beim Start ausgegeben wird.
Für einen späteren Containerbetrieb kann explizit `8080 0.0.0.0` angegeben werden.
Die Lernversion hat keine Authentifizierung und bindet deshalb standardmäßig nur lokal.

## 2. Demo-Client starten

In einem zweiten Terminal vom Repository-Stamm:

```powershell
java -cp forgemind-mcp/target/forgemind-mcp-0.1.0-SNAPSHOT-all.jar de.deringo.forgemind.mcp.McpDemoClient http://127.0.0.1:8080 ForgeMind
```

In der IDE: `de.deringo.forgemind.mcp.McpDemoClient` starten.
Optionale Argumente: `[base-url] [name]`. Ohne Argumente werden
`http://127.0.0.1:8080` und `World` verwendet. Die Basis-URL wird ohne `/mcp`
angegeben; der Client ergänzt diesen Endpunkt.
Andere MCP-Clients verwenden als Streamable-HTTP-Endpunkt die vollständige URL
`http://127.0.0.1:8080/mcp`.

Der Demo-Client startet keinen Server und führt folgende Schritte aus:

1. `initialize`: Protokoll und Fähigkeiten aushandeln.
2. `tools/list`: Toolbeschreibung und Eingabeschema abfragen.
3. `tools/call`: `hello` mit dem Namen aufrufen.
4. MCP-Sitzung schließen; der Server bleibt für weitere Clients verfügbar.

Erwartete Ausgabe (SDK-Logs können zusätzlich auf stderr erscheinen):

```text
Connected to: forgemind-mcp
Protocol: 2025-11-25
Available tools:
- hello: Returns a friendly Hello greeting for the given name.
Calling hello with name: ForgeMind
Hello, ForgeMind!
Connection closed.
```

## Tool und Tests

`hello` erwartet `{"name":"World"}` und liefert `Hello, World!` als MCP-TextContent.
`name` ist ein erforderlicher String mit mindestens einem Zeichen;
zusätzliche Parameter sind nicht erlaubt.

Die Integrationstests verwenden echte HTTP-Verbindungen auf einem freien lokalen
Port. Sie prüfen Initialisierung, Tool-Auflistung, Aufruf, ungültige Parameter,
mehrere aufeinanderfolgende Clients und den Demo-Client als separaten Java-Prozess.

ForgeMinds Agentenschleife ist noch nicht angebunden.

## Windows-JDK-Hinweis

In der Entwicklungsumgebung trat beim Öffnen eines Java-NIO-Selectors
`Unable to establish loopback connection` mit `UnixDomainSockets.connect` auf.
Die Tests liefen mit dem JDK-Fallback auf TCP-Loopback erfolgreich. Nur falls der
Fehler auch bei dir auftritt, kann vorübergehend in beiden Terminals gesetzt werden:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/forgemind-nonexistent-socket-dir'
```

Der angegebene Ordner muss nicht existieren (nicht anlegen), damit das JDK auf TCP
zurückfällt. Die Einstellung gilt nur für Java-Prozesse aus diesem Terminal;
mit `Remove-Item Env:JAVA_TOOL_OPTIONS` lässt sie sich wieder entfernen.
Sie ist kein Teil der Serverkonfiguration und unter Linux nicht erforderlich.

SDK-Dokumentation: https://java.sdk.modelcontextprotocol.io/latest/server/
