package dev.amartya.promptregistry.exception;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String id) {
        super("Template not found: " + id);
    }
}
