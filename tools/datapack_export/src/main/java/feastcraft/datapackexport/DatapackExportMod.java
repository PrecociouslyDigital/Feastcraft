package feastcraft.datapackexport;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Writes the pack's merged data to the directory named by {@value #OUTPUT_VARIABLE} when the title
 * screen first opens, then quits. It does nothing unless that environment variable is set.
 */
@Mod(DatapackExportMod.MOD_ID)
public final class DatapackExportMod {
    public static final String MOD_ID = "feastcraft_datapack_export";
    // An environment variable rather than a system property: a launcher passes its environment on to the game.
    static final String OUTPUT_VARIABLE = "FEASTCRAFT_DATAPACK_EXPORT";

    public DatapackExportMod() {
        String output = System.getenv(OUTPUT_VARIABLE);
        if (output == null || FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        Path directory = Path.of(output);
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Opening event) -> {
            if (event.getNewScreen() instanceof TitleScreen) {
                Minecraft minecraft = Minecraft.getInstance();
                // Every mod has finished loading by now.
                minecraft.tell(() -> {
                    MergedData.write(directory);
                    minecraft.stop();
                });
            }
        });
    }
}
