package dev.amartya.promptregistry.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VariableType {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean");

    private final String jsonValue;

    VariableType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String toJson() {
        return jsonValue;
    }

    @JsonCreator
    public static VariableType fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (VariableType type : values()) {
            if (type.jsonValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown variable type: " + value);
    }
}
