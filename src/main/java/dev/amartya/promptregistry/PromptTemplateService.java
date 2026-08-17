package dev.amartya.promptregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.amartya.promptregistry.entity.PromptTemplateEntity;
import dev.amartya.promptregistry.entity.PromptTemplateVersionEntity;
import dev.amartya.promptregistry.entity.VariableSchemaEntity;
import dev.amartya.promptregistry.exception.TemplateNotFoundException;
import dev.amartya.promptregistry.exception.TemplateValidationException;
import dev.amartya.promptregistry.exception.TemplateVersionNotFoundException;
import dev.amartya.promptregistry.model.CreateVersionRequest;
import dev.amartya.promptregistry.model.PromptTemplate;
import dev.amartya.promptregistry.model.RenderPromptResponse;
import dev.amartya.promptregistry.model.RenderPromptValidationError;
import dev.amartya.promptregistry.model.RenderRequest;
import dev.amartya.promptregistry.model.TemplateVersionDetail;
import dev.amartya.promptregistry.model.TemplateVersionSummary;
import dev.amartya.promptregistry.model.VariableChange;
import dev.amartya.promptregistry.model.VariableChangeSnapshot;
import dev.amartya.promptregistry.model.VariableSchema;
import dev.amartya.promptregistry.model.VariableType;
import dev.amartya.promptregistry.model.VersionDiff;
import dev.amartya.promptregistry.repository.PromptTemplateRepository;
import dev.amartya.promptregistry.repository.PromptTemplateVersionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromptTemplateService {

    static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");
    private static final Set<String> CONTROL_KEYWORDS = Set.of("else", "this", "lookup", "log");

    @Inject
    PromptTemplateRepository templateRepository;

    @Inject
    PromptTemplateVersionRepository versionRepository;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "promptplayground.render.max-template-length", defaultValue = "10000")
    int maxTemplateLength;

    @Transactional
    public PromptTemplate create(PromptTemplate request) {
        assertValidTemplate(request.getTemplate(), request.getVariables());

        Instant now = Instant.now();
        PromptTemplateEntity parent = new PromptTemplateEntity();
        parent.setId(UUID.randomUUID().toString());
        parent.setName(request.getName());
        parent.setCreatedOn(now);

        PromptTemplateVersionEntity version = newVersion(parent, 1, request.getTemplate(), request.getVariables(), now);
        parent.getVersions().add(version);
        templateRepository.persist(parent);
        return toDto(parent, version);
    }

    @Transactional
    public PromptTemplate get(String id) {
        PromptTemplateEntity parent = requireTemplate(id);
        PromptTemplateVersionEntity latest = latestVersion(parent);
        return toDto(parent, latest);
    }

    @Transactional
    public List<PromptTemplate> list() {
        return templateRepository.listAll().stream()
                .map(parent -> toDto(parent, latestVersion(parent)))
                .toList();
    }

    @Transactional
    public void delete(String id) {
        PromptTemplateEntity parent = requireTemplate(id);
        templateRepository.delete(parent);
    }

    @Transactional
    public RenderPromptResponse render(String id, RenderRequest request) {
        return renderTemplate(get(id), request);
    }

    @Transactional
    public PromptTemplate createVersion(String id, CreateVersionRequest request) {
        PromptTemplateEntity parent = templateRepository.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (parent == null) {
            throw new TemplateNotFoundException(id);
        }
        assertValidTemplate(request.getTemplate(), request.getVariables());
        int next = versionRepository.maxVersionNumber(id) + 1;
        PromptTemplateVersionEntity version = newVersion(
                parent, next, request.getTemplate(), request.getVariables(), Instant.now());
        parent.getVersions().add(version);
        versionRepository.persist(version);
        return toDto(parent, version);
    }

    @Transactional
    public List<TemplateVersionSummary> listVersions(String id) {
        requireTemplate(id);
        return versionRepository.listByTemplateIdOrdered(id).stream()
                .map(version -> {
                    TemplateVersionSummary summary = new TemplateVersionSummary();
                    summary.setVersionNumber(version.getVersionNumber());
                    summary.setCreatedOn(version.getCreatedOn());
                    return summary;
                })
                .toList();
    }

    @Transactional
    public TemplateVersionDetail getVersion(String id, int versionNumber) {
        requireTemplate(id);
        PromptTemplateVersionEntity version = requireVersion(id, versionNumber);
        TemplateVersionDetail detail = new TemplateVersionDetail();
        detail.setVersion(version.getVersionNumber());
        detail.setTemplate(version.getTemplateText());
        detail.setVariables(toVariableDtos(version.getVariables()));
        detail.setCreatedOn(version.getCreatedOn());
        return detail;
    }

    @Transactional
    public RenderPromptResponse renderVersion(String id, int versionNumber, RenderRequest request) {
        PromptTemplateEntity parent = requireTemplate(id);
        PromptTemplateVersionEntity version = requireVersion(id, versionNumber);
        return renderTemplate(toDto(parent, version), request);
    }

    @Transactional
    public VersionDiff diff(String id, int fromVersion, int toVersion) {
        requireTemplate(id);
        PromptTemplateVersionEntity from = requireVersion(id, fromVersion);
        PromptTemplateVersionEntity to = requireVersion(id, toVersion);
        List<VariableSchema> fromVars = toVariableDtos(from.getVariables());
        List<VariableSchema> toVars = toVariableDtos(to.getVariables());

        VersionDiff diff = new VersionDiff();
        diff.setTemplateId(id);
        diff.setFromVersion(fromVersion);
        diff.setToVersion(toVersion);
        diff.setFromTemplate(from.getTemplateText());
        diff.setToTemplate(to.getTemplateText());
        diff.setTemplateTextChanged(!Objects.equals(from.getTemplateText(), to.getTemplateText()));
        diff.setVariableChanges(computeVariableChanges(fromVars, toVars));
        return diff;
    }

    PromptTemplateVersionEntity requireVersion(String templateId, int versionNumber) {
        return versionRepository.findByTemplateIdAndVersion(templateId, versionNumber)
                .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionNumber));
    }

    void assertValidTemplate(String template, List<VariableSchema> variables) {
        if (template.length() > maxTemplateLength) {
            throw new TemplateValidationException(
                    "Template exceeds max length of " + maxTemplateLength);
        }

        Set<String> declared = variables == null
                ? Set.of()
                : variables.stream()
                        .map(VariableSchema::getName)
                        .collect(Collectors.toSet());

        List<String> undeclared = extractVariableNames(template).stream()
                .filter(name -> !declared.contains(name))
                .sorted()
                .toList();
        if (!undeclared.isEmpty()) {
            throw new TemplateValidationException(
                    "Undeclared variable(s): " + String.join(", ", undeclared));
        }
    }

    List<VariableChange> computeVariableChanges(List<VariableSchema> fromVars, List<VariableSchema> toVars) {
        Map<String, VariableSchema> fromMap = indexByName(fromVars);
        Map<String, VariableSchema> toMap = indexByName(toVars);
        Set<String> names = new TreeSet<>();
        names.addAll(fromMap.keySet());
        names.addAll(toMap.keySet());

        List<VariableChange> changes = new ArrayList<>();
        for (String name : names) {
            VariableSchema from = fromMap.get(name);
            VariableSchema to = toMap.get(name);
            if (from == null) {
                changes.add(variableChange(name, "added", null, snapshot(to)));
            } else if (to == null) {
                changes.add(variableChange(name, "removed", snapshot(from), null));
            } else {
                if (!Objects.equals(from.getType(), to.getType())) {
                    changes.add(variableChange(name, "typeChanged", snapshot(from), snapshot(to)));
                }
                if (from.isRequired() != to.isRequired()) {
                    changes.add(variableChange(name, "requiredChanged", snapshot(from), snapshot(to)));
                }
                if (!defaultsEqual(from.getDefaultValue(), to.getDefaultValue())) {
                    changes.add(variableChange(name, "defaultChanged", snapshot(from), snapshot(to)));
                }
            }
        }
        return changes;
    }

    private static Map<String, VariableSchema> indexByName(List<VariableSchema> schemas) {
        Map<String, VariableSchema> map = new LinkedHashMap<>();
        if (schemas == null) {
            return map;
        }
        for (VariableSchema schema : schemas) {
            map.put(schema.getName(), schema);
        }
        return map;
    }

    private static boolean defaultsEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        return Objects.equals(left, right);
    }

    private static VariableChangeSnapshot snapshot(VariableSchema schema) {
        VariableChangeSnapshot snap = new VariableChangeSnapshot();
        snap.setType(schema.getType() == null ? null : schema.getType().toJson());
        snap.setRequired(schema.isRequired());
        snap.setDefaultValue(schema.getDefaultValue());
        return snap;
    }

    private static VariableChange variableChange(
            String name,
            String changeType,
            VariableChangeSnapshot from,
            VariableChangeSnapshot to) {
        VariableChange change = new VariableChange();
        change.setName(name);
        change.setChangeType(changeType);
        change.setFrom(from);
        change.setTo(to);
        return change;
    }

    RenderPromptResponse renderTemplate(PromptTemplate template, RenderRequest request) {
        Map<String, Object> provided = request == null || request.getVariables() == null
                ? Map.of()
                : request.getVariables();
        Map<String, Object> effective = new HashMap<>(provided);
        List<RenderPromptValidationError> errors = new ArrayList<>();
        List<VariableSchema> schemas = template.getVariables() == null
                ? List.of()
                : template.getVariables();

        for (VariableSchema schema : schemas) {
            String name = schema.getName();
            boolean present = provided.containsKey(name);
            Object raw = present ? provided.get(name) : null;

            if (schema.isRequired() && isEmpty(raw, present)) {
                errors.add(new RenderPromptValidationError(name, "Required variable is missing"));
                continue;
            }

            if (present && !isEmpty(raw, true)) {
                validateType(name, raw, schema.getType(), errors);
            } else if (!present && schema.getDefaultValue() != null) {
                Object defaultValue = schema.getDefaultValue();
                effective.put(name, defaultValue);
                validateType(name, defaultValue, schema.getType(), errors);
            }
        }

        if (!errors.isEmpty()) {
            return new RenderPromptResponse(null, errors);
        }
        return new RenderPromptResponse(substitute(template.getTemplate(), effective), List.of());
    }

    PromptTemplateEntity requireTemplate(String id) {
        PromptTemplateEntity parent = templateRepository.findById(id);
        if (parent == null) {
            throw new TemplateNotFoundException(id);
        }
        return parent;
    }

    PromptTemplateVersionEntity latestVersion(PromptTemplateEntity parent) {
        return parent.getVersions().stream()
                .max(Comparator.comparingInt(PromptTemplateVersionEntity::getVersionNumber))
                .orElseThrow(() -> new TemplateNotFoundException(parent.getId()));
    }

    PromptTemplateVersionEntity newVersion(
            PromptTemplateEntity parent,
            int versionNumber,
            String templateText,
            List<VariableSchema> variables,
            Instant createdOn) {
        PromptTemplateVersionEntity version = new PromptTemplateVersionEntity();
        version.setTemplate(parent);
        version.setVersionNumber(versionNumber);
        version.setTemplateText(templateText);
        version.setCreatedOn(createdOn);
        version.setVariables(toVariableEntities(variables, version));
        return version;
    }

    PromptTemplate toDto(PromptTemplateEntity parent, PromptTemplateVersionEntity version) {
        int latest = latestVersion(parent).getVersionNumber();
        PromptTemplate dto = new PromptTemplate();
        dto.setId(parent.getId());
        dto.setName(parent.getName());
        dto.setTemplate(version.getTemplateText());
        dto.setVariables(toVariableDtos(version.getVariables()));
        dto.setVersion(version.getVersionNumber());
        dto.setLatestVersion(latest);
        return dto;
    }

    private List<VariableSchemaEntity> toVariableEntities(
            List<VariableSchema> schemas,
            PromptTemplateVersionEntity version) {
        List<VariableSchemaEntity> result = new ArrayList<>();
        if (schemas == null) {
            return result;
        }
        for (VariableSchema schema : schemas) {
            VariableSchemaEntity entity = new VariableSchemaEntity();
            entity.setVersion(version);
            entity.setName(schema.getName());
            entity.setType(schema.getType());
            entity.setRequired(schema.isRequired());
            if (schema.getDefaultValue() != null) {
                try {
                    entity.setDefaultValueJson(objectMapper.writeValueAsString(schema.getDefaultValue()));
                } catch (JsonProcessingException e) {
                    throw new TemplateValidationException(
                            "Could not serialize default for variable " + schema.getName());
                }
            }
            result.add(entity);
        }
        return result;
    }

    private List<VariableSchema> toVariableDtos(List<VariableSchemaEntity> entities) {
        List<VariableSchema> result = new ArrayList<>();
        if (entities == null) {
            return result;
        }
        for (VariableSchemaEntity entity : entities) {
            VariableSchema dto = new VariableSchema();
            dto.setName(entity.getName());
            dto.setType(entity.getType());
            dto.setRequired(entity.isRequired());
            if (entity.getDefaultValueJson() != null) {
                try {
                    dto.setDefaultValue(objectMapper.readValue(entity.getDefaultValueJson(), Object.class));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(
                            "Could not deserialize default for variable " + entity.getName(), e);
                }
            }
            result.add(dto);
        }
        return result;
    }

    private static boolean isEmpty(Object value, boolean present) {
        if (!present) {
            return true;
        }
        return value == null || "".equals(value);
    }

    private static void validateType(
            String name,
            Object value,
            VariableType expected,
            List<RenderPromptValidationError> errors) {
        if (expected == null || matchesType(expected, value)) {
            return;
        }
        String actual = typeName(value);
        errors.add(new RenderPromptValidationError(
                name,
                "Type mismatch: expected " + expected.toJson() + " but got " + actual,
                expected.toJson(),
                actual));
    }

    private static boolean matchesType(VariableType expected, Object value) {
        return switch (expected) {
            case STRING -> value instanceof String;
            case INTEGER -> isInteger(value);
            case NUMBER -> isNumber(value);
            case BOOLEAN -> isBoolean(value);
        };
    }

    private static boolean isInteger(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return true;
        }
        if (value instanceof String text) {
            try {
                Integer.parseInt(text);
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean isNumber(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String text) {
            try {
                Double.parseDouble(text);
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean isBoolean(Object value) {
        if (value instanceof Boolean) {
            return true;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text);
        }
        return false;
    }

    private static String typeName(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "integer";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return value.getClass().getSimpleName();
    }

    private String substitute(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement;
            if (CONTROL_KEYWORDS.contains(name)) {
                replacement = matcher.group(0);
            } else {
                Object value = variables.get(name);
                replacement = value == null ? "" : String.valueOf(value);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    List<String> extractVariableNames(String template) {
        if (template == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!CONTROL_KEYWORDS.contains(name) && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }
}
