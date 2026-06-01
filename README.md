# Mystery Escape Room (Season 3)

Single-player point-and-click escape room game. Java 17 + Spring Boot 3 backend, Java Swing frontend, H2 embedded database. AP Computer Science final project — student: Abhishri Das.

## Status

Phase 1 (MVP) — **complete**. 102 tests pass (86 backend + 16 frontend). All `design.md §20` acceptance boxes are green.

## Documentation

- **[`idea.md`](./idea.md)** — vision, scope, theme, AP CS rubric mapping, resolved architectural decisions, MVP build order.
- **[`design.md`](./design.md)** — implementation-ready blueprint: DDL, DTOs, REST API reference, sequence diagrams, config files, acceptance criteria.

Read `idea.md` first to understand **what** and **why**. Read `design.md` when implementing for the **how**.

## Prerequisites

- JDK 17 (Temurin recommended). **Important on macOS:** the system default JVM may be older. Set `JAVA_HOME` explicitly before every Maven command:
  ```
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
  ```
- Maven 3.8+
- (Optional) An IDE that understands Maven multi-module projects: IntelliJ IDEA, Eclipse, or VS Code with the Java extension pack.

## Quickstart

From the repo root:

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
  mvn --offline clean test
```

Run the backend (terminal 1):

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
  mvn -pl backend spring-boot:run
```

The backend binds to `http://127.0.0.1:8080`. Verify it is up:

```
curl http://127.0.0.1:8080/api/health
```

Run the frontend (terminal 2, after the backend is up):

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
  mvn -pl frontend exec:java -Dexec.mainClass=com.abhishri.escape.ui.EscapeRoomApp
```

The Swing window opens to the Entry Foyer scene. Click "New Game" to start. Full walkthrough of the golden path is in `idea.md` §3.

## Project Layout

See `design.md` §14 for the full tree. Quick reference:

```
game-java-dev/
├── backend/                  Spring Boot REST API + JPA + H2
├── frontend/                 Swing client + HTTP client
├── data/                     H2 file DB (created at runtime; gitignored)
├── saves/                    JSON save snapshots (created at runtime; gitignored)
├── logs/                     Application logs (gitignored)
├── idea.md                   What and why
├── design.md                 How (implementation blueprint)
└── README.md                 This file
```

## Troubleshooting

### "Database may be already in use" on backend startup

H2 in file mode holds an exclusive OS-level lock on `./data/escaperoom.mv.db`. If you accidentally launch a second backend instance (a second `mvn spring-boot:run` in another terminal, or a leftover process from a previous run), the second one fails to start with this error.

**Fix:**

1. Check for a stray `java` process holding the lock:
   - Windows: `Get-Process java` in PowerShell, or check Task Manager.
   - macOS/Linux: `ps aux | grep java`.
2. Stop the orphaned process.
3. If H2 still complains after the process is gone, delete the lock file: `./data/escaperoom.mv.db.lock.db` (only the `.lock.db` file — never the main `.mv.db`).
4. Restart the backend.

### Frontend opens but shows "Connection refused" or blank scene

The backend is not running, or it's running on a different port. Confirm with `curl http://127.0.0.1:8080/api/health`. If that fails, restart the backend before re-launching the frontend.

### Changing `world.json` doesn't take effect

`WorldSeedService` only seeds the H2 database **if a row is missing**. To force a re-seed after editing `world.json`:

1. Stop the backend.
2. Delete the `./data/` directory.
3. Restart the backend. The new `world.json` content is loaded into a fresh database.

### Tests fail with "Port 8080 already in use"

An integration test is trying to start the backend while a manually-launched backend is still running on `:8080`. Stop the manual backend before running `mvn test`.

### H2 console shows no tables

The backend hasn't finished its first startup yet (Spring needs a moment to create the schema). Wait until the console log shows `Tomcat started on port 8080` and refresh `http://127.0.0.1:8080/h2-console`. Login JDBC URL must be `jdbc:h2:file:./data/escaperoom` — leave username `sa`, password blank.

## License

Project code is for educational use as part of an AP Computer Science final project. Any third-party art assets are tracked with their original license in `frontend/src/main/resources/art/CREDITS.md` (added in Phase 2 when real art replaces placeholders).
