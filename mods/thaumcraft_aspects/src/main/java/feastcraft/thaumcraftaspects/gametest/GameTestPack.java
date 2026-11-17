package feastcraft.thaumcraftaspects.gametest;

import feastcraft.thaumcraftaspects.ThaumcraftAspects;
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
 * Aspect entries for GameTests only. The pack is offered only to a GameTest server, and players
 * never see it.
 */
public final class GameTestPack {
    private static final String ROOT = "gametest_pack";

    private GameTestPack() {
    }

    public static void addWhenTesting(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !ForgeGameTestHooks.isGametestServer()) {
            return;
        }
        Path root = ModList.get().getModFileById(ThaumcraftAspects.MOD_ID).getFile().findResource(ROOT);
        event.addRepositorySource(packs -> {
            Pack pack = Pack.readMetaAndCreate(ThaumcraftAspects.id(ROOT).toString(), Component.literal("Thaumcraft Aspects GameTests"), true,
                    id -> new PathPackResources(id, root, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) {
                packs.accept(pack);
            }
        });
    }
}
