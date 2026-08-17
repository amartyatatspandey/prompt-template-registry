# Mini Prompt Template Registry

[![CI](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
<!-- Replace OWNER/REPO with the GitHub path after this project is pushed. -->

A small Quarkus REST service that stores prompt templates with an append-only version history and renders them with declared variable schemas — required fields, defaults, type checks, and `{{varName}}` substitution.

Storage is H2-backed (in-memory JDBC, zero-config — no external database server). Versioning exists because it mirrors the actual scope of the Apicurio Registry Prompt Template Playground mentorship project this demo supports: version history and structural diff.

The render response shape (`rendered`, `validationErrors` with `variableName` / `message` / `expectedType` / `actualType`) deliberately mirrors Apicurio Registry's `RenderPromptResponse` and `RenderPromptValidationError` DTOs, so this is the same contract the Registry UI already consumes.

Requires Java 21. If `java -version` is not 21, point `JAVA_HOME` at a JDK 21 install first.

## Run

```shell
./mvnw quarkus:dev
```

- Swagger UI: http://localhost:8080/q/swagger-ui
- OpenAPI: http://localhost:8080/q/openapi

```shell
./mvnw test
```

## Examples

Create a template (version 1):

```shell
curl -s -X POST http://localhost:8080/templates \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "greeting",
    "template": "Hello, {{name}}!",
    "variables": [
      { "name": "name", "type": "string", "required": true }
    ]
  }'
```

Render the latest version successfully (replace `ID` with the `id` from create):

```shell
curl -s -X POST http://localhost:8080/templates/ID/render \
  -H 'Content-Type: application/json' \
  -d '{ "variables": { "name": "Ada" } }'
```

Render with a missing required variable — HTTP 200, `rendered` is `null`, `validationErrors` names the field:

```shell
curl -s -X POST http://localhost:8080/templates/ID/render \
  -H 'Content-Type: application/json' \
  -d '{ "variables": {} }'
```

Create a second version:

```shell
curl -s -X POST http://localhost:8080/templates/ID/versions \
  -H 'Content-Type: application/json' \
  -d '{
    "template": "Hello, {{name}} in {{city}}!",
    "variables": [
      { "name": "name", "type": "string", "required": true },
      { "name": "city", "type": "string", "required": false }
    ]
  }'
```

List versions:

```shell
curl -s http://localhost:8080/templates/ID/versions
```

Render version 1 explicitly (still v1 content even though v2 exists):

```shell
curl -s -X POST http://localhost:8080/templates/ID/versions/1/render \
  -H 'Content-Type: application/json' \
  -d '{ "variables": { "name": "Ada" } }'
```

Diff v1 → v2:

```shell
curl -s http://localhost:8080/templates/ID/versions/1/diff/2
```
