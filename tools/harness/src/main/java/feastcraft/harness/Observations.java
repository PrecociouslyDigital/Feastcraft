package feastcraft.harness;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** What a player could see: recent chat and action-bar lines, and a snapshot of the screen and world. */
final class Observations {
    private record Line(long seq, String kind, String text) {
    }

    private static final int KEPT_LINES = 512;
    private static final Field BOSS_EVENTS = bossEventsField();

    private static final Deque<Line> lines = new ArrayDeque<>();
    private static long nextSeq;

    private Observations() {
    }

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent event) {
        boolean actionBar = event instanceof ClientChatReceivedEvent.System system && system.isOverlay();
        append(actionBar ? "actionbar" : "chat", event.getMessage().getString());
    }

    private static synchronized void append(String kind, String text) {
        lines.addLast(new Line(nextSeq++, kind, text));
        if (lines.size() > KEPT_LINES) {
            lines.removeFirst();
        }
    }

    /** The sequence number the next line will get, so a caller can ask for everything after now. */
    static synchronized long nextSeq() {
        return nextSeq;
    }

    static synchronized JsonObject linesSince(long seq) {
        JsonArray array = new JsonArray();
        lines.stream().filter(line -> line.seq() >= seq).forEach(line -> {
            JsonObject json = new JsonObject();
            json.addProperty("seq", line.seq());
            json.addProperty("kind", line.kind());
            json.addProperty("text", line.text());
            array.add(json);
        });
        JsonObject result = new JsonObject();
        result.add("lines", array);
        result.addProperty("next", nextSeq);
        return result;
    }

    /** Must run on the client thread. */
    static JsonObject status(Minecraft minecraft) {
        JsonObject status = new JsonObject();
        Screen screen = minecraft.screen;
        status.addProperty("screen", screen == null ? null : screen.getClass().getSimpleName());
        status.add("buttons", buttons(screen));
        LocalPlayer player = minecraft.player;
        status.addProperty("inWorld", player != null && minecraft.level != null);
        if (player == null || minecraft.level == null || minecraft.gameMode == null) {
            return status;
        }

        status.addProperty("dimension", minecraft.level.dimension().location().toString());
        status.add("pos", vector(player.getX(), player.getY(), player.getZ()));
        status.addProperty("yaw", player.getYRot());
        status.addProperty("pitch", player.getXRot());
        status.addProperty("gameMode", minecraft.gameMode.getPlayerMode().getName());
        status.addProperty("health", player.getHealth());
        status.addProperty("alive", player.isAlive());
        status.add("lookingAt", lookingAt(minecraft, minecraft.hitResult));
        status.addProperty("selectedSlot", player.getInventory().selected);
        status.add("hotbar", hotbar(player));
        status.add("bossBars", bossBars(minecraft.gui.getBossOverlay()));
        return status;
    }

    private static JsonArray buttons(Screen screen) {
        JsonArray labels = new JsonArray();
        if (screen != null) {
            screen.children().stream()
                    .filter(AbstractButton.class::isInstance)
                    .map(AbstractButton.class::cast)
                    .filter(button -> button.visible)
                    .forEach(button -> labels.add(button.getMessage().getString()));
        }
        return labels;
    }

    private static JsonElement lookingAt(Minecraft minecraft, HitResult hit) {
        JsonObject json = new JsonObject();
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            json.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            json.add("pos", blockPos(pos));
            JsonObject properties = new JsonObject();
            state.getValues().forEach((property, value) -> properties.addProperty(property.getName(), value.toString()));
            json.add("state", properties);
            return json;
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            json.addProperty("entity", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            json.addProperty("id", entity.getId());
            return json;
        }
        return JsonNull.INSTANCE;
    }

    private static JsonArray hotbar(LocalPlayer player) {
        JsonArray slots = new JsonArray();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            JsonObject json = new JsonObject();
            json.addProperty("slot", slot);
            json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            json.addProperty("count", stack.getCount());
            json.addProperty("nbt", stack.getTag() == null ? null : stack.getTag().toString());
            slots.add(json);
        }
        return slots;
    }

    private static JsonArray bossBars(BossHealthOverlay overlay) {
        JsonArray bars = new JsonArray();
        bossEvents(overlay).values().forEach(event -> {
            JsonObject json = new JsonObject();
            json.addProperty("name", event.getName().getString());
            json.addProperty("progress", event.getProgress());
            bars.add(json);
        });
        return bars;
    }

    static JsonArray blockPos(BlockPos pos) {
        JsonArray array = new JsonArray();
        array.add(pos.getX());
        array.add(pos.getY());
        array.add(pos.getZ());
        return array;
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, LerpingBossEvent> bossEvents(BossHealthOverlay overlay) {
        try {
            return (Map<UUID, LerpingBossEvent>) BOSS_EVENTS.get(overlay);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // The overlay keeps its bars private. It has exactly one map, so this finds it without depending
    // on the field's name, which differs between dev and production mappings.
    private static Field bossEventsField() {
        Field field = Arrays.stream(BossHealthOverlay.class.getDeclaredFields())
                .filter(candidate -> candidate.getType() == Map.class)
                .findFirst()
                .orElseThrow();
        field.setAccessible(true);
        return field;
    }
}
