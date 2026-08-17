package dev.amartya.promptregistry.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PromptTemplate {

    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String template;

    @NotNull
    private List<@Valid VariableSchema> variables = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<VariableSchema> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableSchema> variables) {
        this.variables = variables;
    }
}
