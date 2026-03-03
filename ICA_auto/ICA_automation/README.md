# Selenium Java VS Code Starter (No manual browser downloads)

This is a ready-to-open VS Code project for your **unchanged** code. Open it in VS Code and run `Main`.

## Project layout

```
selenium-vscode-starter/
├── .vscode/
│   ├── launch.json
│   └── settings.json
├── src/
│   └── Main.java
└── pom.xml
```

> **Note**: Your `pom.xml` intentionally uses a custom source directory (`src`). `Main.java` is placed there exactly as your code expects.

## Prerequisites

- **Java 21 JDK** installed and on PATH. Check with:
  - Windows/macOS/Linux: `java -version` (should show `21.x`)
- **Google Chrome** installed (your code uses `ChromeDriver`).
- **Network access** to allow Maven to resolve dependencies and Selenium to fetch the matching driver automatically (no manual web downloads needed).
- IBM network/VPN access for `https://remea.ica.ibm.com/...` (as your automation logs in there).

> You do **not** need to manually download any drivers. Selenium 4+ uses Selenium Manager to fetch drivers automatically at runtime.

## VS Code extensions to install

- **Extension Pack for Java** (`ms-vscode.vscode-java-pack`) – includes:
  - Language Support for Java™ by Red Hat
  - Debugger for Java
  - Test Runner for Java
  - Maven for Java
  - Project Manager for Java
- (Optional) **XML** by Red Hat (`redhat.vscode-xml`) – better `pom.xml` authoring.

## How to run (VS Code)

1. Open the folder `selenium-vscode-starter` in VS Code.
2. Allow the Java extensions to load. They will read `pom.xml` and resolve Maven dependencies automatically.
3. Open `src/Main.java` and click **Run ▶** near the `main` method **or** press **F5** and pick **Run Main**.
4. On first launch, Selenium may auto-download the matching ChromeDriver. No manual browser download is required.
5. A small floating button appears; click it to start your GUI flow and Selenium session.

### Command-line (optional)
If you prefer, from this folder you can also run:

```
# compile
mvn clean compile

# run via VS Code debugger (recommended) or with:
# (VS Code handles classpath for you; CLI running would require exec plugin or manual classpath)
```

## Notes & Tips

- If Maven cannot resolve `selenium-java:4.40.0` from Maven Central, update to the latest 4.x in `pom.xml` (e.g., `4.23.0+`). If you want me to keep your exact POM, ping me and I'll adjust with an `exec-maven-plugin` to allow `mvn exec:java` without modifying your code.
- The automation targets an IBM internal URL and assumes SSO steps; ensure you're on the corporate network.
- On Linux with Wayland, some Swing/Chrome focus behaviors can differ; if the app does not appear on top, try XWayland or set your window manager to allow "always on top".
