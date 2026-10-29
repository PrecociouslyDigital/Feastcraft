package feastcraft.expeditions.data;

import com.google.common.hash.Hashing;
import feastcraft.expeditions.Expeditions;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.data.PackOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Generates the ember charcoal texture as a mask recolor of vanilla charcoal: each opaque pixel's
 * luminance is mapped onto an ember palette, so the shape is vanilla's and only the color changes.
 * Vanilla's item textures are pulled from the client-extra jar that moddev places in the project's
 * build artifacts, so no packed asset is needed.
 */
final class EmberCharcoalTexture implements DataProvider {
    /** Stops of the {@code luminance -> (r, g, b)} ramp, from cold ash to white-hot ember. */
    private static final int[][] RAMP = {
            {0x1A, 0x0A, 0x05},
            {0x4A, 0x15, 0x0A},
            {0x8C, 0x2F, 0x0E},
            {0xC9, 0x6A, 0x1C},
            {0xFF, 0xC8, 0x6B},
    };

    private final PackOutput output;

    EmberCharcoalTexture(PackOutput output, ExistingFileHelper existingFiles) {
        this.output = output;
        // The item model provider validates its textures against this helper, but the PNG only
        // materializes during this run. Track it as generated so validation sees it either way.
        existingFiles.trackGenerated(Expeditions.id("item/ember_charcoal"), PackType.CLIENT_RESOURCES, ".png", "textures");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> generate(cache));
    }

    private void generate(CachedOutput cache) {
        try (InputStream in = openVanillaCharcoal()) {
            BufferedImage source = ImageIO.read(in);
            int width = source.getWidth();
            int height = source.getHeight();
            BufferedImage recolored = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha == 0) {
                        continue;
                    }
                    int red = (argb >>> 16) & 0xFF;
                    int green = (argb >>> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
                    int[] color = emberColor(luminance);
                    recolored.setRGB(x, y, (alpha << 24) | (color[0] << 16) | (color[1] << 8) | color[2]);
                }
            }
            // Route the bytes through the cache: datagen purges any output file it did not track,
            // so a direct ImageIO.write would survive the run but be deleted before it ends.
            var bytes = new java.io.ByteArrayOutputStream();
            if (!ImageIO.write(recolored, "png", bytes)) {
                throw new IllegalStateException("no PNG writer claimed ember_charcoal.png");
            }
            byte[] data = bytes.toByteArray();
            Path path = output.getOutputFolder()
                    .resolve("assets/" + Expeditions.MOD_ID + "/textures/item/ember_charcoal.png");
            cache.writeIfNeeded(path, data, Hashing.sha1().hashBytes(data));
        } catch (IOException e) {
            throw new IllegalStateException("could not recolor vanilla charcoal", e);
        }
    }

    /** Finds vanilla charcoal.png: on the classpath if present, else inside the moddev client-extra jar. */
    private InputStream openVanillaCharcoal() throws IOException {
        InputStream onClasspath = EmberCharcoalTexture.class.getResourceAsStream("/assets/minecraft/textures/item/charcoal.png");
        if (onClasspath != null) {
            return onClasspath;
        }
        Path artifacts = output.getOutputFolder().toAbsolutePath().getParent().getParent().getParent()
                .resolve("build/moddev/artifacts");
        if (!Files.isDirectory(artifacts)) {
            throw new IllegalStateException("neither the classpath nor " + artifacts + " has vanilla charcoal.png");
        }
        try (Stream<Path> jars = Files.list(artifacts)) {
            Path clientExtra = jars.filter(name -> name.getFileName().toString().startsWith("client-extra-"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("no client-extra jar in " + artifacts));
            try (JarFile jar = new JarFile(clientExtra.toFile())) {
                var entry = jar.getJarEntry("assets/minecraft/textures/item/charcoal.png");
                if (entry == null) {
                    throw new IllegalStateException("vanilla charcoal.png missing from " + clientExtra.getFileName());
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    return new java.io.ByteArrayInputStream(in.readAllBytes());
                }
            }
        }
    }

    /** Piecewise-linear lookup over {@link #RAMP}; {@code luminance} is 0..255. */
    private static int[] emberColor(int luminance) {
        if (luminance <= 0) {
            return RAMP[0];
        }
        if (luminance >= 255) {
            return RAMP[RAMP.length - 1];
        }
        double position = luminance / 255.0 * (RAMP.length - 1);
        int lower = (int) position;
        double fraction = position - lower;
        int[] from = RAMP[lower];
        int[] to = RAMP[lower + 1];
        return new int[]{
                (int) Math.round(from[0] + (to[0] - from[0]) * fraction),
                (int) Math.round(from[1] + (to[1] - from[1]) * fraction),
                (int) Math.round(from[2] + (to[2] - from[2]) * fraction),
        };
    }

    @Override
    public String getName() {
        return "Ember Charcoal Texture";
    }
}
