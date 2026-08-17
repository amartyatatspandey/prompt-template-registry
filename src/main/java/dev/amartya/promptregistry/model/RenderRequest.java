package dev.amartya.promptregistry.model;

import java.util.HashMap;
import java.util.Map;

public class RenderRequest {

    private Map<String, Object> variables = new HashMap<>();

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
