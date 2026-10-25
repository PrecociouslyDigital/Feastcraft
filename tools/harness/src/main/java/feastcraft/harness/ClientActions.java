package feastcraft.harness;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * What the control API can do. Keyed inputs (use, attack, sneak, select) go through the same key
 * mappings a real player's keyboard and mouse do, so vanilla handles them as it would a real press.
 */
final class ClientActions {
    private static final String HOST_GAME_DIR_PROPERTY = "feastcraft.harness.hostGameDir";

    // Replies take a server round trip, which can be seconds while the server generates terrain.
    private static final int MAX_REPLY_TICKS = 100;
    private static final int REPLY_QUIET_TICKS = 5;
    private static final int MAX_WAIT_TICKS = 20 * 60 * 10;
    private static final int MAX_FIND_RADIUS = 64;
    private static final int MAX_FOUND = 8;
    private static final Pattern SCREENSHOT_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private ClientActions() {
    }

    static CompletableFuture<JsonElement> status(Params params) {
        return ClientTicks.onTick(() -> Observations.status(minecraft()));
    }

    static CompletableFuture<JsonElement> chat(Params params) {
        long since = params.has("since") ? params.integer("since", 0, Integer.MAX_VALUE) : 0;
        return CompletableFuture.completedFuture(Observations.linesSince(since));
    }

    static CompletableFuture<JsonElement> command(Params params) {
        String command = params.string("c");
        String withoutSlash = command.startsWith("/") ? command.substring(1) : command;
        return replyTo(player -> player.connection.sendCommand(withoutSlash));
    }

    static CompletableFuture<JsonElement> use(Params params) {
        return replyTo(player -> KeyMapping.click(minecraft().options.keyUse.getKey()));
    }

    static CompletableFuture<JsonElement> attack(Params params) {
        return replyTo(player -> KeyMapping.click(minecraft().options.keyAttack.getKey()));
    }

    /** Turns to {@code yaw} and {@code pitch} in degrees, or to face the point {@code x}, {@code y}, {@code z}. */
    static CompletableFuture<JsonElement> look(Params params) {
        if (params.has("x")) {
            Vec3 target = new Vec3(params.decimal("x"), params.decimal("y"), params.decimal("z"));
            return thenStatus(player -> player.lookAt(EntityAnchorArgument.Anchor.EYES, target));
        }
        float yaw = Mth.wrapDegrees(params.decimal("yaw"));
        float pitch = Mth.clamp(params.decimal("pitch"), -90, 90);
        return thenStatus(player -> {
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yRotO = yaw;
            player.xRotO = pitch;
        });
    }

    /** The nearest blocks of a kind within {@code radius} blocks horizontally, as far as the client has them loaded. */
    static CompletableFuture<JsonElement> find(Params params) {
        ResourceLocation id = ResourceLocation.tryParse(params.string("block"));
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            throw Refusal.badRequest("no block " + params.string("block"));
        }
        int radius = params.integer("radius", 1, MAX_FIND_RADIUS);
        return ClientTicks.onTick(() -> {
            BlockPos center = playerInWorld().blockPosition();
            ClientLevel level = minecraft().level;
            JsonArray found = new JsonArray();
            BlockPos.betweenClosedStream(
                            center.getX() - radius, level.getMinBuildHeight(), center.getZ() - radius,
                            center.getX() + radius, level.getMaxBuildHeight() - 1, center.getZ() + radius)
                    .filter(pos -> level.getBlockState(pos).is(block))
                    .map(BlockPos::immutable)
                    .sorted(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                    .limit(MAX_FOUND)
                    .forEach(pos -> found.add(Observations.blockPos(pos)));
            JsonObject result = new JsonObject();
            result.add("found", found);
            return result;
        });
    }

    static CompletableFuture<JsonElement> sneak(Params params) {
        boolean down = params.bool("down");
        return thenStatus(player -> minecraft().options.keyShift.setDown(down));
    }

    static CompletableFuture<JsonElement> select(Params params) {
        int slot = params.integer("slot", 0, 8);
        return thenStatus(player -> KeyMapping.click(minecraft().options.keyHotbarSlots[slot].getKey()));
    }

    static CompletableFuture<JsonElement> waitTicks(Params params) {
        int ticks = params.integer("ticks", 0, MAX_WAIT_TICKS);
        return ClientTicks.afterTicks(ticks).thenCompose(done -> ClientTicks.onTick(() -> Observations.status(minecraft())));
    }

    /** Presses the button with this label on the open screen, such as "Respawn" on the death screen. */
    static CompletableFuture<JsonElement> click(Params params) {
        String label = params.string("label");
        return ClientTicks.onTick(() -> {
            if (minecraft().screen == null) {
                throw Refusal.conflict("no screen is open");
            }
            AbstractButton button = minecraft().screen.children().stream()
                    .filter(AbstractButton.class::isInstance)
                    .map(AbstractButton.class::cast)
                    .filter(candidate -> candidate.visible && candidate.getMessage().getString().equals(label))
                    .findFirst()
                    .orElseThrow(() -> Refusal.conflict("no button labelled " + label));
            if (!button.active) {
                throw Refusal.conflict(label + " is not active yet");
            }
            button.onPress();
            return null;
        }).thenCompose(pressed -> ClientTicks.onTick(() -> Observations.status(minecraft())));
    }

    static CompletableFuture<JsonElement> screenshot(Params params) {
        String name = params.matching("name", SCREENSHOT_NAME);
        boolean hud = !params.has("hud") || params.bool("hud");
        return ClientTicks.onTick(() -> {
            boolean wasHidden = minecraft().options.hideGui;
            minecraft().options.hideGui = !hud;
            return wasHidden;
        }).thenCompose(wasHidden -> ClientTicks.onFrame(() -> {
            String fileName = name + ".png";
            try (NativeImage image = Screenshot.takeScreenshot(minecraft().getMainRenderTarget())) {
                File directory = new File(minecraft().gameDirectory, "screenshots");
                directory.mkdirs();
                image.writeToFile(new File(directory, fileName));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                minecraft().options.hideGui = wasHidden;
            }
            String gameDirectory = System.getProperty(HOST_GAME_DIR_PROPERTY, minecraft().gameDirectory.getAbsolutePath());
            JsonObject result = new JsonObject();
            result.addProperty("path", Path.of(gameDirectory, "screenshots", fileName).toString());
            return result;
        }));
    }

    static CompletableFuture<JsonElement> quit(Params params) {
        return ClientTicks.onTick(() -> {
            minecraft().stop();
            return new JsonObject();
        });
    }

    /**
     * Acts as the player, then collects the chat and action-bar lines that come back: it waits for
     * the first, then until none have arrived for a few ticks.
     */
    private static CompletableFuture<JsonElement> replyTo(Consumer<LocalPlayer> action) {
        return ClientTicks.onTick(() -> {
            long since = Observations.nextSeq();
            action.accept(playerInWorld());
            return since;
        }).thenCompose(since -> replies(since, since, 0, 0));
    }

    private static CompletableFuture<JsonElement> replies(long since, long seenUpTo, int waited, int quiet) {
        return ClientTicks.afterTicks(1).thenCompose(tick -> {
            long next = Observations.nextSeq();
            int nowQuiet = next == seenUpTo ? quiet + 1 : 0;
            boolean settled = next > since && nowQuiet >= REPLY_QUIET_TICKS;
            return settled || waited + 1 >= MAX_REPLY_TICKS
                    ? CompletableFuture.completedFuture(Observations.linesSince(since))
                    : replies(since, next, waited + 1, nowQuiet);
        });
    }

    /** Acts as the player, then reports the status once a tick has applied the change. */
    private static CompletableFuture<JsonElement> thenStatus(Consumer<LocalPlayer> action) {
        return ClientTicks.onTick(() -> {
            action.accept(playerInWorld());
            return null;
        }).thenCompose(done -> ClientTicks.onTick(() -> Observations.status(minecraft())));
    }

    private static LocalPlayer playerInWorld() {
        LocalPlayer player = minecraft().player;
        if (player == null || minecraft().level == null) {
            throw Refusal.conflict("not in a world");
        }
        return player;
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
