package feastcraft.harness;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/** A request's query parameters, read as the type each endpoint expects. */
record Params(Map<String, String> values) {
    String string(String name) {
        return optional(name).orElseThrow(() -> Refusal.badRequest("missing parameter " + name));
    }

    String matching(String name, Pattern pattern) {
        String value = string(name);
        if (!pattern.matcher(value).matches()) {
            throw Refusal.badRequest(name + " must match " + pattern);
        }
        return value;
    }

    int integer(String name, int min, int max) {
        int value = parsed(name, Integer::parseInt, "an integer");
        if (value < min || value > max) {
            throw Refusal.badRequest(name + " must be between " + min + " and " + max);
        }
        return value;
    }

    float decimal(String name) {
        return parsed(name, Float::parseFloat, "a number");
    }

    boolean bool(String name) {
        return switch (string(name)) {
            case "true", "1" -> true;
            case "false", "0" -> false;
            default -> throw Refusal.badRequest(name + " must be true or false");
        };
    }

    Optional<String> optional(String name) {
        return Optional.ofNullable(values.get(name));
    }

    boolean has(String name) {
        return values.containsKey(name);
    }

    private <T> T parsed(String name, Function<String, T> parse, String description) {
        try {
            return parse.apply(string(name));
        } catch (NumberFormatException e) {
            throw Refusal.badRequest(name + " must be " + description);
        }
    }
}
