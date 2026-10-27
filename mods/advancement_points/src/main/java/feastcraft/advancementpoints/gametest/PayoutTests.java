package feastcraft.advancementpoints.gametest;

import feastcraft.advancementpoints.AdvancementPoints;
import feastcraft.advancementpoints.Payout;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Against the rules in {@link GameTestPack}: story advancements pay 1 per task and 2 per goal,
 * story/root pays another 10, and recipe unlocks would pay 100 if they counted.
 */
@GameTestHolder(AdvancementPoints.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PayoutTests {
    private PayoutTests() {
    }

    @GameTest(template = "empty")
    public static void pointsFollowAdvancementsBothWays(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.detached(helper);
        Category category = SkillsAPI.getCategory(ResourceLocation.parse(GameTestPack.CATEGORY)).orElse(null);
        helper.assertTrue(category != null, "the test skill tree did not load");

        grant(player, "story/root");
        expectPoints(helper, category, player, 11, "after the root, a task worth 1 plus a bonus of 10");
        grant(player, "story/cure_zombie_villager");
        expectPoints(helper, category, player, 13, "after a goal worth 2");
        revoke(player, "story/cure_zombie_villager");
        expectPoints(helper, category, player, 11, "after revoking the goal");
        grant(player, "recipes/misc/stick");
        expectPoints(helper, category, player, 11, "after a recipe unlock, which has no display");

        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void settlingAgainChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.detached(helper);
        Category category = SkillsAPI.getCategory(ResourceLocation.parse(GameTestPack.CATEGORY)).orElseThrow();
        grant(player, "story/root");
        Payout.settle(player, false);
        Payout.settle(player, true);
        expectPoints(helper, category, player, 11, "after settling twice more");
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    private static Advancement advancement(ServerPlayer player, String path) {
        return player.server.getAdvancements().getAdvancement(ResourceLocation.withDefaultNamespace(path));
    }

    private static void grant(ServerPlayer player, String path) {
        Advancement advancement = advancement(player, path);
        advancement.getCriteria().keySet().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
    }

    private static void revoke(ServerPlayer player, String path) {
        Advancement advancement = advancement(player, path);
        advancement.getCriteria().keySet().forEach(criterion -> player.getAdvancements().revoke(advancement, criterion));
    }

    private static void expectPoints(GameTestHelper helper, Category category, ServerPlayer player, int expected, String when) {
        int actual = category.getPoints(player, Payout.SOURCE);
        helper.assertTrue(actual == expected, "expected " + expected + " points " + when + ", got " + actual);
    }
}
