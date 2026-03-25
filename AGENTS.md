# AGENTS.md

## Destructive commands

- Do not run `mvn clean`, `gradle clean`, `rm`, or any command that deletes build outputs, caches, generated files, or other artifacts without explicit user approval.
- If a clean rebuild would help verify a change, ask first and explain why it is needed.

## Documentation

- Always write Javadoc for all Java classes, enums, methods, and fields, including `private` ones.
- When adding or changing Java code, update the corresponding Javadoc at the same time.
