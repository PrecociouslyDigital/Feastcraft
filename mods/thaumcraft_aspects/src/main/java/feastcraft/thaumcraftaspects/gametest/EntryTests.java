package feastcraft.thaumcraftaspects.gametest;

import feastcraft.thaumcraftaspects.AspectEntries;
import feastcraft.thaumcraftaspects.AspectEntry;
import feastcraft.thaumcraftaspects.AspectEntry.Aspects;
import feastcraft.thaumcraftaspects.AspectEntry.Kind;
import feastcraft.thaumcraftaspects.AspectEntry.Parent;
import feastcraft.thaumcraftaspects.ThaumcraftAspects;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Against the entries in {@link GameTestPack}, where only the first three are well formed and name ids the game has. */
@GameTestHolder(ThaumcraftAspects.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntryTests {
    private EntryTests() {
    }

    @GameTest(template = "empty")
    public static void onlyWellFormedEntriesForRegisteredIdsLoad(GameTestHelper helper) {
        Set<AspectEntry> expected = Set.of(
                new AspectEntry(Kind.ITEM, vanilla("oak_stairs"), new Parent(vanilla("oak_planks"))),
                new AspectEntry(Kind.BLOCK, vanilla("diamond_ore"), new Aspects(Map.of("terra", 1, "lucrum", 2))),
                new AspectEntry(Kind.ENTITY, vanilla("pig"), new Aspects(Map.of("bestia", 2))));
        Set<AspectEntry> loaded = new HashSet<>(AspectEntries.entries());
        helper.assertTrue(loaded.equals(expected), "expected " + expected + ", loaded " + loaded);
        helper.succeed();
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
