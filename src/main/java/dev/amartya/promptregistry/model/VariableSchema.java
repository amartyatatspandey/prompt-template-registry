package dev.amartya.promptregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VariableSchema {

    @NotBlank
    private String name;

    @NotNull
    private VariableType type;

    private boolean required;

    @JsonProperty("default")
    private Object defaultValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
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
