package dev.amartya.promptregistry.model;

import java.util.ArrayList;
import java.util.List;

public class RenderPromptResponse {

    private String rendered;
    private List<RenderPromptValidationError> validationErrors = new ArrayList<>();

    public RenderPromptResponse() {
    }

    public RenderPromptResponse(String rendered, List<RenderPromptValidationError> validationErrors) {
        this.rendered = rendered;
        this.validationErrors = validationErrors;
    }

    public String getRendered() {
        return rendered;
    }

    public void setRendered(String rendered) {
        this.rendered = rendered;
    }

    public List<RenderPromptValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<RenderPromptValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
