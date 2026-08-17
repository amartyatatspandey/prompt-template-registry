package dev.amartya.promptregistry.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TemplateVersionDetail {

    private int version;
    private String template;
    private List<VariableSchema> variables = new ArrayList<>();
    private Instant createdOn;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public Instant getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }
}
