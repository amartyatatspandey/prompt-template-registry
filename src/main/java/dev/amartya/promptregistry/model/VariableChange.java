package dev.amartya.promptregistry.model;

public class VariableChange {

    private String name;
    private String changeType;
    private VariableChangeSnapshot from;
    private VariableChangeSnapshot to;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public VariableChangeSnapshot getFrom() {
        return from;
    }

    public void setFrom(VariableChangeSnapshot from) {
        this.from = from;
    }

    public VariableChangeSnapshot getTo() {
        return to;
    }

    public void setTo(VariableChangeSnapshot to) {
        this.to = to;
    }
}
