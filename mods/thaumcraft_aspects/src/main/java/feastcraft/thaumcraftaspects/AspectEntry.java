package feastcraft.thaumcraftaspects;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/** A subject that gets aspects, either copied from a parent of the same kind or listed outright. */
public record AspectEntry(Kind kind, ResourceLocation id, Source source) {
    public enum Kind {
        ITEM, BLOCK, ENTITY
    }

    public sealed interface Source {
    }

    public record Parent(ResourceLocation id) implements Source {
    }

    public record Aspects(Map<String, Integer> amounts) implements Source {
        public Aspects {
            if (amounts.isEmpty()) {
                throw new IllegalArgumentException("no aspects");
            }
            amounts.forEach((aspect, amount) -> {
                if (!aspect.matches("[a-z]+")) {
                    throw new IllegalArgumentException("an aspect is a lowercase name like metallum, got " + aspect);
                }
                if (amount < 1) {
                    throw new IllegalArgumentException(aspect + " needs an amount of at least 1, got " + amount);
                }
            });
            amounts = Map.copyOf(new TreeMap<>(amounts));
        }
    }
}
