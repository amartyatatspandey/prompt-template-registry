package dev.amartya.promptregistry.entity;

import dev.amartya.promptregistry.model.VariableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "variable_schema")
public class VariableSchemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptTemplateVersionEntity version;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VariableType type;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "default_value_json")
    private String defaultValueJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PromptTemplateVersionEntity getVersion() {
        return version;
    }

    public void setVersion(PromptTemplateVersionEntity version) {
        this.version = version;
    }

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

    public String getDefaultValueJson() {
        return defaultValueJson;
    }

    public void setDefaultValueJson(String defaultValueJson) {
        this.defaultValueJson = defaultValueJson;
    }
}
