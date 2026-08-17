package dev.amartya.promptregistry;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class PromptTemplateVersionResourceTest {

    @Test
    void createVersionMakesLatestWinOnGet() {
        String id = createV1();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Hello, {{name}} from v2!",
                          "variables": [
                            { "name": "name", "type": "string", "required": true }
                          ]
                        }
                        """)
                .when()
                .post("/templates/{id}/versions", id)
                .then()
                .statusCode(201)
                .body("version", equalTo(2))
                .body("template", equalTo("Hello, {{name}} from v2!"));

        given()
                .when()
                .get("/templates/{id}", id)
                .then()
                .statusCode(200)
                .body("version", equalTo(2))
                .body("template", equalTo("Hello, {{name}} from v2!"));
    }

    @Test
    void listVersionsReturnsBothInOrder() {
        String id = createV1();
        createV2(id);

        given()
                .when()
                .get("/templates/{id}/versions", id)
                .then()
                .statusCode(200)
                .body("versionNumber", equalTo(java.util.List.of(1, 2)));
    }

    @Test
    void getOutOfRangeVersionReturns404JsonWithoutStackTrace() {
        String id = createV1();

        given()
                .when()
                .get("/templates/{id}/versions/99", id)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsString("99"))
                .body(not(containsString("Exception")))
                .body(not(containsString("at ")));
    }

    @Test
    void renderSpecificOlderVersionAfterNewerExists() {
        String id = createV1();
        createV2(id);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "variables": { "name": "Ada" } }
                        """)
                .when()
                .post("/templates/{id}/versions/1/render", id)
                .then()
                .statusCode(200)
                .body("rendered", equalTo("Hello, Ada!"))
                .body("validationErrors", empty());
    }

    @Test
    void diffReportsAddedVariable() {
        String id = createV1();
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Hello, {{name}} in {{city}}!",
                          "variables": [
                            { "name": "name", "type": "string", "required": true },
                            { "name": "city", "type": "string", "required": false }
                          ]
                        }
                        """)
                .when()
                .post("/templates/{id}/versions", id)
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/templates/{id}/versions/1/diff/2", id)
                .then()
                .statusCode(200)
                .body("fromVersion", equalTo(1))
                .body("toVersion", equalTo(2))
                .body("templateTextChanged", equalTo(true))
                .body("variableChanges", hasSize(1))
                .body("variableChanges[0].name", equalTo("city"))
                .body("variableChanges[0].changeType", equalTo("added"))
                .body("variableChanges[0].from", nullValue())
                .body("variableChanges[0].to.type", equalTo("string"));
    }

    @Test
    void diffReportsRequiredChanged() {
        String id = createV1();
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Hello, {{name}}!",
                          "variables": [
                            { "name": "name", "type": "string", "required": false }
                          ]
                        }
                        """)
                .when()
                .post("/templates/{id}/versions", id)
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/templates/{id}/versions/1/diff/2", id)
                .then()
                .statusCode(200)
                .body("templateTextChanged", equalTo(false))
                .body("variableChanges", hasSize(1))
                .body("variableChanges[0].name", equalTo("name"))
                .body("variableChanges[0].changeType", equalTo("requiredChanged"));
    }

    @Test
    void diffIdenticalVersionsHasEmptyVariableChanges() {
        String id = createV1();
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Hello, {{name}}!",
                          "variables": [
                            { "name": "name", "type": "string", "required": true }
                          ]
                        }
                        """)
                .when()
                .post("/templates/{id}/versions", id)
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/templates/{id}/versions/1/diff/2", id)
                .then()
                .statusCode(200)
                .body("templateTextChanged", equalTo(false))
                .body("variableChanges", empty());
    }

    @Test
    void diffWithMissingVersionReturns404() {
        String id = createV1();

        given()
                .when()
                .get("/templates/{id}/versions/1/diff/99", id)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsString("99"));
    }

    @Test
    void createVersionWithUndeclaredPlaceholderReturns400() {
        String id = createV1();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Visit {{city}}",
                          "variables": []
                        }
                        """)
                .when()
                .post("/templates/{id}/versions", id)
                .then()
                .statusCode(400)
                .body("message", containsString("city"));
    }

    @Test
    void createVersionOnUnknownTemplateReturns404() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Hello",
                          "variables": []
                        }
                        """)
                .when()
                .post("/templates/does-not-exist/versions")
                .then()
                .statusCode(404)
                .body("message", containsString("does-not-exist"));
    }

    private String createV1() {
        return given()
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
                .extract()
                .path("id");
    }

    private void createV2(String id) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "template": "Hello, {{name}} from v2!",
                          "variables": [
                            { "name": "name", "type": "string", "required": true }
                          ]
                        }
                        """)
                .when()
                .post("/templates/{id}/versions", id)
                .then()
                .statusCode(201);
    }
}
