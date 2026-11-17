package feastcraft.datapackexport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import feastcraft.datapackexport.mixin.FolderPackAccessor;
import feastcraft.datapackexport.mixin.ServerResourcesAccessor;
import feastcraft.datapackexport.mixin.ZipPackAccessor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Unit;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.forgespi.language.IModFileInfo;

/**
 * The data a fresh world would load, as one datapack-shaped directory:
 * <ul>
 * <li>{@code data/} holds each file as the game resolves it: the highest pack's copy, or for a tag
 * every pack's entries merged.
 * <li>{@code sources.tsv} names the packs that supply each file, lowest first.
 * <li>{@code packs.txt} lists the packs, lowest first.
 * <li>{@code recipes.tsv} lists the recipes the game is left with after loading those files.
 * </ul>
 */
final class MergedData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private MergedData() {
    }

    /** Writes the export to {@code output}, finishing with {@code sources.tsv}. */
    static void write(Path output) {
        Minecraft minecraft = Minecraft.getInstance();
        PackRepository repository = new PackRepository(new ServerPacksSource());
        ModLoader.get().postEvent(new AddPackFindersEvent(PackType.SERVER_DATA, repository::addPackFinder));
        // The load the Create World screen does before there is a world.
        WorldLoader.PackConfig freshWorld = new WorldLoader.PackConfig(repository, WorldDataConfiguration.DEFAULT, false, true);
        WorldLoader.InitConfig load = new WorldLoader.InitConfig(freshWorld, Commands.CommandSelection.INTEGRATED, 2);
        CompletableFuture<Unit> written = WorldLoader.load(load,
                context -> new WorldLoader.DataLoadOutput<>(Unit.INSTANCE, context.datapackDimensions()),
                (resources, serverResources, registries, unit) -> {
                    try (resources) {
                        Files.createDirectories(output);
                        bindItemTags(serverResources, registries.compositeAccess());
                        RecipeList.write(output, serverResources.getRecipeManager(), registries.compositeAccess());
                        writeFiles(output, resources);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return unit;
                }, Util.backgroundExecutor(), minecraft);
        // The load finishes on this thread, so it has to keep running its tasks while it waits.
        minecraft.managedBlock(written::isDone);
        written.join();
    }

    /**
     * Binds item tags so recipe ingredients that name a tag aren't empty; a server would do this as
     * it starts. Item tags only, because Slice and Dice hooks binding all tags and throws here.
     */
    private static void bindItemTags(ReloadableServerResources serverResources, RegistryAccess registries) {
        ((ServerResourcesAccessor) serverResources).feastcraft_datapack_export$tagManager().getResult().stream()
                .filter(result -> result.key().equals(Registries.ITEM))
                .forEach(result -> bind(registries, result));
    }

    private static <T> void bind(RegistryAccess registries, TagManager.LoadResult<T> result) {
        Map<TagKey<T>, List<Holder<T>>> tags = new HashMap<>();
        result.tags().forEach((id, members) -> tags.put(TagKey.create(result.key(), id), List.copyOf(members)));
        registries.registryOrThrow(result.key()).bindTags(tags);
    }

    private static void writeFiles(Path output, CloseableResourceManager resources) throws IOException {
        List<PackResources> packs = resources.listPacks().toList();
        Files.write(output.resolve("packs.txt"), packs.stream().map(PackResources::packId).toList());

        Map<ResourceLocation, List<Resource>> stacks = new TreeMap<>();
        for (String directory : topLevelDirectories(packs)) {
            // This mod's own jar is a pack too.
            stacks.putAll(resources.listResourceStacks(directory, file -> !file.getNamespace().equals(DatapackExportMod.MOD_ID)));
        }
        // Written last: the script treats its presence as a finished export.
        try (BufferedWriter sources = Files.newBufferedWriter(output.resolve("sources.tsv"))) {
            for (Map.Entry<ResourceLocation, List<Resource>> entry : stacks.entrySet()) {
                ResourceLocation file = entry.getKey();
                List<Resource> stack = entry.getValue();
                Path target = output.resolve("data").resolve(file.getNamespace()).resolve(file.getPath());
                Files.createDirectories(target.getParent());
                Optional<String> listKey = mergedListKey(file);
                if (listKey.isPresent()) {
                    Files.writeString(target, GSON.toJson(mergeLists(stack, listKey.get())));
                } else {
                    try (InputStream content = stack.get(stack.size() - 1).open()) {
                        Files.copy(content, target);
                    }
                }
                sources.write(file + "\t" + stack.stream().map(Resource::sourcePackId).collect(Collectors.joining(" < ")));
                sources.newLine();
            }
        }
    }

    /**
     * The directories directly under {@code data/<namespace>/} in any mod, folder pack or zip pack.
     * The game can only list files below a named directory, so these are the directories the export lists.
     */
    private static Set<String> topLevelDirectories(List<PackResources> packs) throws IOException {
        Set<String> directories = new TreeSet<>();
        for (IModFileInfo mod : ModList.get().getModFiles()) {
            addTopLevelDirectories(mod.getFile().findResource("data"), directories);
        }
        for (PackResources pack : packs) {
            if (pack instanceof FolderPackAccessor folder) {
                addTopLevelDirectories(folder.feastcraft_datapack_export$root().resolve("data"), directories);
            } else if (pack instanceof ZipPackAccessor zip) {
                try (FileSystem contents = FileSystems.newFileSystem(zip.feastcraft_datapack_export$file().toPath())) {
                    addTopLevelDirectories(contents.getPath("data"), directories);
                }
            }
        }
        return directories;
    }

    private static void addTopLevelDirectories(Path data, Set<String> directories) throws IOException {
        if (!Files.isDirectory(data)) {
            return;
        }
        for (Path namespace : subdirectories(data)) {
            for (Path directory : subdirectories(namespace)) {
                // A path inside a jar can keep its trailing slash.
                directories.add(directory.getFileName().toString().replace("/", ""));
            }
        }
    }

    private static List<Path> subdirectories(Path directory) throws IOException {
        try (Stream<Path> children = Files.list(directory)) {
            return children.filter(Files::isDirectory).toList();
        }
    }

    /** The key of the list a file builds up across packs, for the files the game merges rather than overrides. */
    private static Optional<String> mergedListKey(ResourceLocation file) {
        if (!file.getPath().endsWith(".json")) {
            return Optional.empty();
        }
        if (file.getPath().startsWith("tags/")) {
            return Optional.of("values");
        }
        if (file.equals(ResourceLocation.fromNamespaceAndPath("forge", "loot_modifiers/global_loot_modifiers.json"))) {
            return Optional.of("entries");
        }
        return Optional.empty();
    }

    /** Each pack adds its entries; {@code replace} drops the lower packs' entries first, and {@code remove} drops single ones. */
    private static JsonObject mergeLists(List<Resource> stack, String listKey) throws IOException {
        Set<JsonElement> merged = new LinkedHashSet<>();
        for (Resource resource : stack) {
            JsonObject json;
            try (BufferedReader reader = resource.openAsReader()) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (JsonParseException | IllegalStateException e) {
                // The game skips a pack's copy it can't read.
                continue;
            }
            if (json.has("replace") && json.get("replace").getAsBoolean()) {
                merged.clear();
            }
            if (json.has(listKey)) {
                json.getAsJsonArray(listKey).forEach(merged::add);
            }
            if (json.has("remove")) {
                json.getAsJsonArray("remove").forEach(merged::remove);
            }
        }
        JsonObject result = new JsonObject();
        result.add(listKey, GSON.toJsonTree(merged));
        return result;
    }
}
