package feastcraft.expeditions.gametest;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Rolls real loot tables until ember charcoal shows up; at these rates, missing it means the modifiers never loaded. */
@GameTestHolder(Expeditions.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EmberLootTests {
    private EmberLootTests() {
    }

    @GameTest(template = "empty", batch = "ember_loot")
    public static void chestsHoldEmberCharcoal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable dungeon = level.getServer().getLootData().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        helper.assertTrue(rollsEmber(dungeon, params, 200), "200 dungeon chests held no ember charcoal");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ember_loot")
    public static void monstersDropEmberCharcoal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, BlockPos.ZERO);
        LootTable drops = level.getServer().getLootData().getLootTable(zombie.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, zombie)
                .withParameter(LootContextParams.ORIGIN, zombie.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        helper.assertTrue(rollsEmber(drops, params, 2_000), "2000 zombies dropped no ember charcoal");
        zombie.discard();
        helper.succeed();
    }

    private static boolean rollsEmber(LootTable table, LootParams params, int rolls) {
        return IntStream.range(0, rolls).anyMatch(roll -> table.getRandomItems(params).stream()
                .anyMatch(stack -> stack.is(ExpeditionsRegistry.EMBER_CHARCOAL.get())));
    }
}
