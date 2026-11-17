package feastcraft.thaumcraftaspects;

import feastcraft.thaumcraftaspects.AspectEntry.Aspects;
import feastcraft.thaumcraftaspects.AspectEntry.Parent;
import github.com.gengyoubo.tcmodern.api.scan.AspectValues;
import github.com.gengyoubo.tcmodern.api.scan.ScanKey;
import github.com.gengyoubo.tcmodern.api.scan.TcScanApi;
import github.com.gengyoubo.tcmodern.api.scan.TcScanRegistry;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The only class that touches TC Modern Lib, so the rest of the mod loads without it. TC Modern Lib
 * resolves these and hands them to Thaumcraft when it syncs datapacks to players.
 */
final class ThaumcraftScans {
    private ThaumcraftScans() {
    }

    static void define(List<AspectEntry> entries) {
        TcScanRegistry registry = TcScanApi.registry();
        for (AspectEntry entry : entries) {
            ScanKey key = key(entry.kind(), entry.id());
            if (entry.source() instanceof Parent parent) {
                registry.inherit(key, key(entry.kind(), parent.id()));
            } else if (entry.source() instanceof Aspects aspects) {
                registry.register(key, new AspectValues(aspects.amounts()));
            }
        }
    }

    private static ScanKey key(AspectEntry.Kind kind, ResourceLocation id) {
        return switch (kind) {
            case ITEM -> ScanKey.item(id.toString());
            case BLOCK -> ScanKey.block(id.toString());
            case ENTITY -> ScanKey.entity(id.toString());
        };
    }
}
