package feastcraft.advancementpoints.gametest;

import feastcraft.advancementpoints.AdvancementPoints;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.ForgeGameTestHooks;

/**
 * A skill tree and point rules for GameTests only. The mod ships no tree of its own, so the pack is
 * offered only to a GameTest server, and players never see it.
 */
public final class GameTestPack {
    static final String CATEGORY = AdvancementPoints.id("test").toString();
    private static final String ROOT = "gametest_pack";

    private GameTestPack() {
    }

    public static void addWhenTesting(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !ForgeGameTestHooks.isGametestServer()) {
            return;
        }
        Path root = ModList.get().getModFileById(AdvancementPoints.MOD_ID).getFile().findResource(ROOT);
        event.addRepositorySource(packs -> {
            Pack pack = Pack.readMetaAndCreate(AdvancementPoints.id(ROOT).toString(), Component.literal("Advancement Points GameTests"), true,
                    id -> new PathPackResources(id, root, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) {
                packs.accept(pack);
            }
        });
    }
}
