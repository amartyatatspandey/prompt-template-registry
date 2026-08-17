package dev.amartya.promptregistry;

import dev.amartya.promptregistry.exception.TemplateNotFoundException;
import dev.amartya.promptregistry.exception.TemplateValidationException;
import dev.amartya.promptregistry.model.PromptTemplate;
import dev.amartya.promptregistry.model.RenderPromptResponse;
import dev.amartya.promptregistry.model.RenderPromptValidationError;
import dev.amartya.promptregistry.model.RenderRequest;
import dev.amartya.promptregistry.model.VariableSchema;
import dev.amartya.promptregistry.model.VariableType;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromptTemplateService {

    static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");
    private static final Set<String> CONTROL_KEYWORDS = Set.of("else", "this", "lookup", "log");

    private final ConcurrentHashMap<String, PromptTemplate> store = new ConcurrentHashMap<>();

    @ConfigProperty(name = "promptplayground.render.max-template-length", defaultValue = "10000")
    int maxTemplateLength;

    public PromptTemplate create(PromptTemplate request) {
        String template = request.getTemplate();
        if (template.length() > maxTemplateLength) {
            throw new TemplateValidationException(
                    "Template exceeds max length of " + maxTemplateLength);
        }

        Set<String> declared = request.getVariables() == null
                ? Set.of()
                : request.getVariables().stream()
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

        PromptTemplate stored = new PromptTemplate();
        stored.setId(UUID.randomUUID().toString());
        stored.setName(request.getName());
        stored.setTemplate(template);
        stored.setVariables(request.getVariables() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getVariables()));
        store.put(stored.getId(), stored);
        return stored;
    }

    public PromptTemplate get(String id) {
        PromptTemplate template = store.get(id);
        if (template == null) {
            throw new TemplateNotFoundException(id);
        }
        return template;
    }

    public List<PromptTemplate> list() {
        return new ArrayList<>(store.values());
    }

    public void delete(String id) {
        if (store.remove(id) == null) {
            throw new TemplateNotFoundException(id);
        }
    }

    public RenderPromptResponse render(String id, RenderRequest request) {
        PromptTemplate template = get(id);
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
                // Every non-keyword placeholder is guaranteed declared by create()'s
                // undeclared-variable check, so a null lookup here only ever means an
                // optional variable with no provided value and no default — resolve it
                // to an empty string rather than leaking raw template syntax into output.
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
