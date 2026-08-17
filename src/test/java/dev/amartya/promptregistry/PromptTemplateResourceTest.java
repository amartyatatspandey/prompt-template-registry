package dev.amartya.promptregistry;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class PromptTemplateResourceTest {

    @Test
    void createValidTemplateReturns201WithId() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "greeting",
                          "template": "Hello, {{name}}!",
                          "variables": [
                            { "name": "name", "type": "string", "required": true }
                          ]
                        }
                        """)
                .when()
                .post("/templates")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("greeting"))
                .body("template", equalTo("Hello, {{name}}!"));
    }

    @Test
    void createTemplateWithUndeclaredPlaceholderReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "travel",
                          "template": "Visit {{city}} this weekend",
                          "variables": []
                        }
                        """)
                .when()
                .post("/templates")
                .then()
                .statusCode(400)
                .body("message", containsString("city"));
    }

    @Test
    void createTemplateWithControlKeywordPlaceholdersDoesNotRequireThemDeclared() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "control-keywords",
                          "template": "{{else}} {{this}} {{lookup}} {{log}}",
                          "variables": []
                        }
                        """)
                .when()
                .post("/templates")
                .then()
                .statusCode(201);
    }

    @Test
    void getUnknownTemplateReturns404JsonWithoutStackTrace() {
        given()
                .when()
                .get("/templates/does-not-exist")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsString("does-not-exist"))
                .body(not(containsString("Exception")))
                .body(not(containsString("at ")));
    }

    @Test
    void createTemplateExceedingMaxLengthReturns400() {
        String tooLong = "x".repeat(81);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "long",
                          "template": "%s",
                          "variables": []
                        }
                        """.formatted(tooLong))
                .when()
                .post("/templates")
                .then()
                .statusCode(400)
                .body("message", containsString("80"));
    }

    @Test
    void createTemplateWithBlankNameReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "",
                          "template": "Hello",
                          "variables": []
                        }
                        """)
                .when()
                .post("/templates")
                .then()
                .statusCode(400);
    }

    @Test
    void renderWithRequiredVariablesReturnsSubstitutedText() {
        String id = createTemplate("""
                {
                  "name": "greeting",
                  "template": "Hello, {{name}}!",
                  "variables": [
                    { "name": "name", "type": "string", "required": true }
                  ]
                }
                """);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": { "name": "Ada" } }
                        """)
                .when()
                .post("/templates/{id}/render", id)
                .then()
                .statusCode(200)
                .body("rendered", equalTo("Hello, Ada!"))
                .body("validationErrors", empty());
    }

    @Test
    void renderMissingRequiredVariableReturns200WithNullRendered() {
        String id = createTemplate("""
                {
                  "name": "greeting",
                  "template": "Hello, {{name}}!",
                  "variables": [
                    { "name": "name", "type": "string", "required": true }
                  ]
                }
                """);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": {} }
                        """)
                .when()
                .post("/templates/{id}/render", id)
                .then()
                .statusCode(200)
                .body("rendered", nullValue())
                .body("validationErrors[0].variableName", equalTo("name"))
                .body("validationErrors[0].message", containsString("Required"));
    }

    @Test
    void renderEmptyStringRequiredVariableReturns200WithNullRendered() {
        String id = createTemplate("""
                {
                  "name": "greeting",
                  "template": "Hello, {{name}}!",
                  "variables": [
                    { "name": "name", "type": "string", "required": true }
                  ]
                }
                """);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": { "name": "" } }
                        """)
                .when()
                .post("/templates/{id}/render", id)
                .then()
                .statusCode(200)
                .body("rendered", nullValue())
                .body("validationErrors[0].variableName", equalTo("name"));
    }

    @Test
    void renderAppliesDeclaredDefaultWhenVariableOmitted() {
        String id = createTemplate("""
                {
                  "name": "greeting",
                  "template": "Hello, {{title}}!",
                  "variables": [
                    { "name": "title", "type": "string", "required": false, "default": "Friend" }
                  ]
                }
                """);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": {} }
                        """)
                .when()
                .post("/templates/{id}/render", id)
                .then()
                .statusCode(200)
                .body("rendered", equalTo("Hello, Friend!"))
                .body("validationErrors", empty());
    }

    @Test
    void renderResolvesUnfilledOptionalVariableToEmptyStringNotRawPlaceholder() {
        String id = createTemplate("""
                {
                  "name": "greeting",
                  "template": "Hello{{title}}, {{name}}!",
                  "variables": [
                    { "name": "name", "type": "string", "required": true },
                    { "name": "title", "type": "string", "required": false }
                  ]
                }
                """);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": { "name": "Ada" } }
                        """)
                .when()
                .post("/templates/{id}/render", id)
                .then()
                .statusCode(200)
                .body("rendered", equalTo("Hello, Ada!"))
                .body("validationErrors", empty());
    }

    @Test
    void renderIntegerTypeMismatchReturns200WithNullRendered() {
        String id = createTemplate("""
                {
                  "name": "counter",
                  "template": "Count: {{count}}",
                  "variables": [
                    { "name": "count", "type": "integer", "required": true }
                  ]
                }
                """);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": { "count": "x" } }
                        """)
                .when()
                .post("/templates/{id}/render", id)
                .then()
                .statusCode(200)
                .body("rendered", nullValue())
                .body("validationErrors[0].variableName", equalTo("count"))
                .body("validationErrors[0].expectedType", equalTo("integer"));
    }

    @Test
    void renderUnknownTemplateReturns404() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": {} }
                        """)
                .when()
                .post("/templates/does-not-exist/render")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsString("does-not-exist"));
    }

    private String createTemplate(String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/templates")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
