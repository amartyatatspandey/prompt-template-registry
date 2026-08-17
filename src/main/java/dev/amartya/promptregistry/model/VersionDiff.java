package dev.amartya.promptregistry.model;

import java.util.ArrayList;
import java.util.List;

public class VersionDiff {

    private String templateId;
    private int fromVersion;
    private int toVersion;
    private String fromTemplate;
    private String toTemplate;
    private boolean templateTextChanged;
    private List<VariableChange> variableChanges = new ArrayList<>();

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public int getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(int fromVersion) {
        this.fromVersion = fromVersion;
    }

    public int getToVersion() {
        return toVersion;
    }

    public void setToVersion(int toVersion) {
        this.toVersion = toVersion;
    }

    public String getFromTemplate() {
        return fromTemplate;
    }

    public void setFromTemplate(String fromTemplate) {
        this.fromTemplate = fromTemplate;
    }

    public String getToTemplate() {
        return toTemplate;
    }

    public void setToTemplate(String toTemplate) {
        this.toTemplate = toTemplate;
    }

    public boolean isTemplateTextChanged() {
        return templateTextChanged;
    }

    public void setTemplateTextChanged(boolean templateTextChanged) {
        this.templateTextChanged = templateTextChanged;
    }

    public List<VariableChange> getVariableChanges() {
        return variableChanges;
    }

    public void setVariableChanges(List<VariableChange> variableChanges) {
        this.variableChanges = variableChanges;
    }
}
