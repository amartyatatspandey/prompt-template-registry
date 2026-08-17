package dev.amartya.promptregistry.exception;

public class TemplateVersionNotFoundException extends RuntimeException {

    public TemplateVersionNotFoundException(String templateId, int version) {
        super("Template version not found: " + templateId + "/" + version);
    }
}
