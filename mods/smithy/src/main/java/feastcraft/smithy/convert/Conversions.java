package feastcraft.smithy.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feastcraft.smithy.Smithy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Builds the Tinkers tool a retired item becomes, from the smithy datapack's
 * `smithy/conversions/<namespace>/<item>.json`. Reloaded with the datapacks, so
 * `tools/smithy` can change the ledger without touching the mod.
 */
public final class Conversions {
    private static Map<ResourceLocation, Conversion> conversions = Map.of();

    private Conversions() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(Conversions::onReload);
    }

    private static void onReload(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Map<ResourceLocation, Conversion>>() {
            @Override
            protected Map<ResourceLocation, Conversion> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<ResourceLocation, Conversion> read = new HashMap<>();
                manager.listResources("smithy/conversions", path -> path.toString().endsWith(".json"))
                        .forEach((id, resource) -> {
                            try (var reader = resource.openAsReader()) {
                                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                                Conversion conversion = Conversion.read(Conversion.source(id), json);
                                read.put(conversion.source(), conversion);
                            } catch (Exception e) {
                                Smithy.LOGGER.error("Smithy can't read conversion {}", id, e);
                            }
                        });
                return read;
            }

            @Override
            protected void apply(Map<ResourceLocation, Conversion> read, ResourceManager manager, ProfilerFiller profiler) {
                conversions = Map.copyOf(read);
                Smithy.LOGGER.info("Smithy loaded {} conversions", read.size());
            }
        });
    }

    /** The one conversion function: a disabled item becomes its Tinkers tool, anything else is itself. */
    public static ItemStack convert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        Conversion conversion = conversions.get(id);
        if (conversion == null) {
            return stack;
        }
        return conversion.build(stack);
    }

    /** One conversion: a tool id, the materials of each part, and the modifiers to add. */
    record Conversion(ResourceLocation source, ResourceLocation tool, List<MaterialId> materials, List<ModifierId> modifiers) {
        /**
         * `smithy/conversions/<namespace>/<name>.json` names the item it
         * converts: the path after the folder is the full item id, since item
         * ids never hold a slash.
         */
        static ResourceLocation source(ResourceLocation conversionFile) {
            String path = conversionFile.getPath();
            String remainder = path.substring("smithy/conversions/".length(), path.length() - ".json".length());
            int slash = remainder.indexOf('/');
            return ResourceLocation.fromNamespaceAndPath(remainder.substring(0, slash), remainder.substring(slash + 1));
        }

        static Conversion read(ResourceLocation source, JsonObject json) {
            String tool = json.get("tool").getAsString();
            List<MaterialId> materials = ids(json.getAsJsonArray("materials"), MaterialId::new);
            List<ModifierId> modifiers = ids(json.getAsJsonArray("modifiers"), ModifierId::new);
            return new Conversion(source, ResourceLocation.parse(tool), materials, modifiers);
        }

        private static <T extends ResourceLocation> List<T> ids(JsonArray array, java.util.function.Function<ResourceLocation, T> of) {
            List<T> read = new ArrayList<>();
            for (JsonElement element : array) {
                read.add(of.apply(ResourceLocation.parse(element.getAsString())));
            }
            return List.copyOf(read);
        }

        /** The Tinkers tool, with the source's damage fraction and custom name carried over. */
        ItemStack build(ItemStack sourceStack) {
            var item = ForgeRegistries.ITEMS.getValue(tool);
            if (!(item instanceof IModifiable modifiable)) {
                Smithy.LOGGER.error("Smithy conversion {} names {} which is not a Tinkers tool", source, tool);
                return sourceStack;
            }
            // add(MaterialVariantId) per id: List<MaterialId> is not a List<MaterialVariantId>.
            MaterialNBT.Builder materialsBuilder = MaterialNBT.builder();
            materials.forEach(materialsBuilder::add);
            MaterialNBT materialNbt = materialsBuilder.build();
            ItemStack built = ToolBuildHandler.buildItemFromMaterials(modifiable, materialNbt);
            ToolStack toolStack = ToolStack.from(built);
            for (ModifierId modifier : modifiers) {
                toolStack.addModifier(modifier, 1);
            }
            keepDamage(sourceStack, toolStack);
            keepCustomName(sourceStack, built);
            return built;
        }

        private static void keepDamage(ItemStack sourceStack, ToolStack toolStack) {
            if (sourceStack.isDamageableItem() && sourceStack.isDamaged()) {
                float fraction = (float) sourceStack.getDamageValue() / sourceStack.getMaxDamage();
                int maxDamage = toolStack.getCurrentDurability() + toolStack.getDamage();
                toolStack.setDamage(Math.min(maxDamage, Math.round(maxDamage * fraction)));
            }
        }

        private static void keepCustomName(ItemStack sourceStack, ItemStack built) {
            CompoundTag tag = sourceStack.getTag();
            if (tag != null && tag.contains("display", Tag.TAG_COMPOUND)) {
                CompoundTag display = tag.getCompound("display");
                if (display.contains("Name", Tag.TAG_STRING)) {
                    CompoundTag builtDisplay = built.getOrCreateTag().getCompound("display");
                    builtDisplay.putString("Name", display.getString("Name"));
                    built.getOrCreateTag().put("display", builtDisplay);
                }
            }
        }
    }
}
