package feastcraft.expeditions;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ExpeditionsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BASE_RADIUS_CHUNKS;

    static {
        var builder = new ForgeConfigSpec.Builder();

        builder.push("base");
        BASE_RADIUS_CHUNKS = builder
                .comment("Chunks from the center chunk to the base's edge. The base is (2r+1)x(2r+1) chunks;",
                        "its world border and its fuel-powered chunk loading both cover exactly that square.")
                .defineInRange("baseRadiusChunks", 4, 0, 32);
        builder.pop();

        SPEC = builder.build();
    }

    private ExpeditionsConfig() {
    }
}
