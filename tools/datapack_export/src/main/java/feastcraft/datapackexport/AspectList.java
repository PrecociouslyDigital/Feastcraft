package feastcraft.datapackexport;

import github.com.gengyoubo.tcmodern.api.scan.AspectValues;
import github.com.gengyoubo.tcmodern.api.scan.ScanKey;
import github.com.gengyoubo.tcmodern.api.scan.TcScanApi;
import github.com.gengyoubo.tcmodern.lib.scan.ScanRules;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code aspects.tsv}: the aspects Thaumcraft and TC Modern Lib give items, blocks and mobs, by kind
 * and id: {@code item	minecraft:iron_ingot	metallum:4}. Entries datapacks add through TC Modern Lib
 * are left out, so a tool that generates those datapacks never reads back its own output.
 */
final class AspectList {
    private AspectList() {
    }

    static void write(Path output) {
        var registry = TcScanApi.registry();
        try {
            Files.createDirectories(output);
            Files.write(output.resolve("aspects.tsv"), ScanRules.server().entrySet().stream()
                    .filter(entry -> !registry.isOverride(entry.getKey()))
                    .map(entry -> line(entry.getKey(), entry.getValue()))
                    .sorted()
                    .toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String line(ScanKey key, AspectValues values) {
        return key.kind().name().toLowerCase(Locale.ROOT) + "\t" + key.id() + "\t" + values.amounts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(aspect -> aspect.getKey() + ":" + aspect.getValue())
                .collect(Collectors.joining(" "));
    }
}
