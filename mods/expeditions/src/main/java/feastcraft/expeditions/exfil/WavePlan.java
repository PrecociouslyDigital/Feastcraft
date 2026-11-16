package feastcraft.expeditions.exfil;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The waves one exfil fights off, from {@code data/<namespace>/feastcraft_expeditions/exfil_waves/*.json}. */
record WavePlan(int weight, List<ResourceKey<Level>> dimensions, List<Wave> waves) {
    static final Codec<WavePlan> CODEC = RecordCodecBuilder.create(plan -> plan.group(
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(WavePlan::weight),
            ExtraCodecs.nonEmptyList(ResourceKey.codec(Registries.DIMENSION).listOf()).fieldOf("dimensions").forGetter(WavePlan::dimensions),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(WavePlan::waves)
    ).apply(plan, WavePlan::new));

    /** {@code at} is how far through the countdown the wave arrives, from 0 (when it is called) up to 1. */
    record Wave(double at, List<WaveSpawn> spawns) {
        static final Codec<Wave> CODEC = RecordCodecBuilder.create(wave -> wave.group(
                ExtraCodecs.validate(Codec.DOUBLE, at -> at >= 0 && at < 1
                        ? DataResult.success(at)
                        : DataResult.error(() -> "at must be in [0, 1), got " + at)).fieldOf("at").forGetter(Wave::at),
                WaveSpawn.CODEC.listOf().fieldOf("spawns").forGetter(Wave::spawns)
        ).apply(wave, Wave::new));
    }

    sealed interface WaveSpawn {
        Codec<WaveSpawn> CODEC = Codec.STRING.partialDispatch("type", spawn -> DataResult.success(spawn.type()), type -> switch (type) {
            case EntitySpawn.TYPE -> DataResult.<Codec<? extends WaveSpawn>>success(EntitySpawn.CODEC);
            case ApotheosisBoss.TYPE -> DataResult.<Codec<? extends WaveSpawn>>success(ApotheosisBoss.CODEC);
            default -> DataResult.error(() -> "unknown spawn type " + type);
        });

        String type();

        /** How likely this spawn is to happen at all when its wave arrives. */
        double chance();
    }

    /**
     * {@code perRaider} multiplies the count by the raiders defending the beacon. {@code effects} last
     * until the countdown ends. {@code minibossChance} rolls each mob into an Apotheosis miniboss.
     */
    record EntitySpawn(EntityType<?> entity, Count count, boolean perRaider, double chance,
                       Optional<CompoundTag> nbt, List<Effect> effects, double minibossChance) implements WaveSpawn {
        static final String TYPE = "entity";
        static final Codec<EntitySpawn> CODEC = RecordCodecBuilder.create(spawn -> spawn.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(EntitySpawn::entity),
                Count.CODEC.optionalFieldOf("count", new Count(1, 1)).forGetter(EntitySpawn::count),
                Codec.BOOL.optionalFieldOf("per_raider", false).forGetter(EntitySpawn::perRaider),
                Codec.doubleRange(0, 1).optionalFieldOf("chance", 1.0).forGetter(EntitySpawn::chance),
                CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(EntitySpawn::nbt),
                Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(EntitySpawn::effects),
                Codec.doubleRange(0, 1).optionalFieldOf("miniboss_chance", 0.0).forGetter(EntitySpawn::minibossChance)
        ).apply(spawn, EntitySpawn::new));

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** A named Apotheosis boss, or without one, any boss Apotheosis allows in the beacon's dimension. */
    record ApotheosisBoss(Optional<ResourceLocation> boss, double chance) implements WaveSpawn {
        static final String TYPE = "apotheosis:boss";
        static final Codec<ApotheosisBoss> CODEC = RecordCodecBuilder.create(spawn -> spawn.group(
                ResourceLocation.CODEC.optionalFieldOf("boss").forGetter(ApotheosisBoss::boss),
                Codec.doubleRange(0, 1).optionalFieldOf("chance", 1.0).forGetter(ApotheosisBoss::chance)
        ).apply(spawn, ApotheosisBoss::new));

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** Written as a number, or as {@code [min, max]} for a uniform pick between the two. */
    record Count(int min, int max) {
        static final Codec<Count> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, ExtraCodecs.NON_NEGATIVE_INT.listOf())
                .comapFlatMap(Count::parse, count -> count.min() == count.max()
                        ? Either.left(count.min())
                        : Either.right(List.of(count.min(), count.max())));

        private static DataResult<Count> parse(Either<Integer, List<Integer>> written) {
            return written.map(
                    exact -> DataResult.success(new Count(exact, exact)),
                    range -> range.size() == 2 && range.get(0) <= range.get(1)
                            ? DataResult.success(new Count(range.get(0), range.get(1)))
                            : DataResult.error(() -> "count must be a number or [min, max] with min <= max, got " + range));
        }

        int sample(RandomSource random) {
            return random.nextIntBetweenInclusive(min, max);
        }
    }

    record Effect(MobEffect effect, int amplifier) {
        static final Codec<Effect> CODEC = RecordCodecBuilder.create(effect -> effect.group(
                BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(Effect::effect),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier)
        ).apply(effect, Effect::new));
    }
}
