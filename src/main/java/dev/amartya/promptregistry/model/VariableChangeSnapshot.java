package dev.amartya.promptregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VariableChangeSnapshot {

    private String type;
    private boolean required;

    @JsonProperty("default")
    private Object defaultValue;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @JsonProperty("default")
    public Object getDefaultValue() {
        return defaultValue;
    }

    @JsonProperty("default")
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
}
