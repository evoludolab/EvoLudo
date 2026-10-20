# AGENTS.md

## Destructive commands

- Do not run `mvn clean`, `gradle clean`, `rm`, or any command that deletes build outputs, caches, generated files, or other artifacts without explicit user approval.
- If a clean rebuild would help verify a change, ask first and explain why it is needed.
- `mvn clean` (or any Maven goal containing `clean`, e.g. `clean install`, `clean package`) is especially dangerous if a Jetty/GWT devmode session is running: it kills that session. Devmode is started via `mvn gwt:devmode`, serves the test page on the port set by the `evoludo.jettyPort` property in the top-level `pom.xml` (currently `8888`), and the GWT code server listens on its default port `9876`. Before running any clean-containing Maven command, check for an active session (e.g. `lsof -i :$(grep -oP '(?<=<evoludo.jettyPort>)[0-9]+' pom.xml)` or `lsof -i :9876`) and call this out explicitly when asking for approval.

## Documentation

- Always write Javadoc for all Java classes, enums, methods, and fields, including `private` ones, except overridden methods.
- When adding or changing Java code, update the corresponding Javadoc at the same time.
