package feastcraft.advancementpoints.rules;

import java.util.regex.Pattern;

/**
 * A {@code namespace:path} advancement id in which {@code *} stands for any run of characters, so
 * {@code mymod:*} is all of a mod's advancements and {@code minecraft:story/*} is one tab of vanilla's.
 */
public final class AdvancementPattern {
    private static final Pattern WELL_FORMED = Pattern.compile("[a-z0-9_.*-]+:[a-z0-9_./*-]+");

    private final String glob;
    private final Pattern regex;

    public AdvancementPattern(String glob) {
        if (!WELL_FORMED.matcher(glob).matches()) {
            throw new IllegalArgumentException("an advancement pattern looks like namespace:path, with * for any run of characters, got " + glob);
        }
        this.glob = glob;
        StringBuilder regex = new StringBuilder();
        for (String literal : glob.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(literal));
        }
        this.regex = Pattern.compile(regex.toString());
    }

    public boolean matches(String advancement) {
        return regex.matcher(advancement).matches();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AdvancementPattern pattern && glob.equals(pattern.glob);
    }

    @Override
    public int hashCode() {
        return glob.hashCode();
    }

    @Override
    public String toString() {
        return glob;
    }
}
