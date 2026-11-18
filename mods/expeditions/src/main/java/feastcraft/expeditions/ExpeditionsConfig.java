package feastcraft.expeditions;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ExpeditionsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue EMBER_ENABLED;
    public static final ForgeConfigSpec.IntValue DROP_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue DROP_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue EXFIL_COUNTDOWN_TICKS;

    static {
        var builder = new ForgeConfigSpec.Builder();

        builder.push("deployment");
        DROP_MIN_DISTANCE = builder
                .comment("Closest a deployment lands to world spawn, in blocks.")
                .defineInRange("dropMinDistance", 1_000, 0, 29_000_000);
        DROP_MAX_DISTANCE = builder
                .comment("Farthest a deployment lands from world spawn, in blocks. Drops are spread evenly by area in between.")
                .defineInRange("dropMaxDistance", 10_000, 0, 29_000_000);
        builder.pop();

        builder.push("exfil");
        EXFIL_COUNTDOWN_TICKS = builder
                .comment("Ticks between calling a beacon and departure. Everyone in its zone when it ends goes home.")
                .defineInRange("countdownTicks", 2_400, 1, 72_000);
        builder.pop();

        SPEC = builder.build();

        // Common so both server and client agree on the gate: the outline renderer is client-side.
        var common = new ForgeConfigSpec.Builder();
        common.push("ember");
        EMBER_ENABLED = common
                .comment("Master switch for ember charcoal and per-hearth fuel. When off, ember charcoal stops",
                        "appearing in loot, every chunk of the base runs without fuel, and hearths are inert.",
                        "Flip this off if the fuel loop turns out not to be fun.")
                .define("enabled", true);
        common.pop();
        COMMON_SPEC = common.build();
    }

    private ExpeditionsConfig() {
    }
}
