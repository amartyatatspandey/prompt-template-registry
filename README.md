# Mini Prompt Template Registry

A small Quarkus REST service that stores prompt templates in memory and renders them with declared variable schemas — required fields, defaults, type checks, and `{{varName}}` substitution.

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

Create a template:

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

Render successfully (replace `ID` with the `id` from create):

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
# prompt-template-registry
