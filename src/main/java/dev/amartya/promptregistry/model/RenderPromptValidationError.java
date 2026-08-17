package dev.amartya.promptregistry.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenderPromptValidationError {

    private String variableName;
    private String message;
    private String expectedType;
    private String actualType;

    public RenderPromptValidationError() {
    }

    public RenderPromptValidationError(String variableName, String message) {
        this.variableName = variableName;
        this.message = message;
    }

    public RenderPromptValidationError(String variableName, String message, String expectedType, String actualType) {
        this.variableName = variableName;
        this.message = message;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        this.expectedType = expectedType;
    }

    public String getActualType() {
        return actualType;
    }

    public void setActualType(String actualType) {
        this.actualType = actualType;
    }
}
