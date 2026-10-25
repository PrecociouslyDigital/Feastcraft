package feastcraft.expeditions;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ExpeditionsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BASE_RADIUS_CHUNKS;
    public static final ForgeConfigSpec.IntValue DROP_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue DROP_MAX_DISTANCE;

    static {
        var builder = new ForgeConfigSpec.Builder();

        builder.push("base");
        BASE_RADIUS_CHUNKS = builder
                .comment("Chunks from the center chunk to the base's edge. The base is (2r+1)x(2r+1) chunks;",
                        "its world border and its fuel-powered chunk loading both cover exactly that square.")
                .defineInRange("baseRadiusChunks", 4, 0, 32);
        builder.pop();

        builder.push("deployment");
        DROP_MIN_DISTANCE = builder
                .comment("Closest a deployment lands to world spawn, in blocks.")
                .defineInRange("dropMinDistance", 1_000, 0, 29_000_000);
        DROP_MAX_DISTANCE = builder
                .comment("Farthest a deployment lands from world spawn, in blocks. Drops are spread evenly by area in between.")
                .defineInRange("dropMaxDistance", 10_000, 0, 29_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private ExpeditionsConfig() {
    }
}
