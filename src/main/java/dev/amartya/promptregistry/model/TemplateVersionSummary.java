package dev.amartya.promptregistry.model;

import java.time.Instant;

public class TemplateVersionSummary {

    private int versionNumber;
    private Instant createdOn;

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }
}
