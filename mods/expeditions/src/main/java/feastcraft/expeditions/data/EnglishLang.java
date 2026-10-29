package feastcraft.expeditions.data;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

final class EnglishLang extends LanguageProvider {
    EnglishLang(PackOutput output) {
        super(output, Expeditions.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.feastcraft_expeditions", "Feastcraft Expeditions");
        add(ExpeditionsRegistry.HEARTH.get(), "Hearth");
        add(ExpeditionsRegistry.DEPARTURE_GATE.get(), "Departure Gate");
        add(ExpeditionsRegistry.EXFIL_BEACON.get(), "Exfil Beacon");
        add(ExpeditionsRegistry.EXFIL_COMPASS.get(), "Exfil Compass");
        add(ExpeditionsRegistry.EMBER_CHARCOAL.get(), "Ember Charcoal");

        add("feastcraft_expeditions.hearth.lit", "A hearth roars to life. Its chunk is running.");
        add("feastcraft_expeditions.hearth.died", "A hearth has gone out. Its chunk's plants and machines have stopped.");
        add("feastcraft_expeditions.hearth.not_in_base", "A hearth only burns in the base.");
        add("feastcraft_expeditions.hearth.remaining", "Fuel remaining: %s");
        add("feastcraft_expeditions.hearth.empty", "The hearth is cold. Feed it fuel to run its chunk.");
        add("feastcraft_expeditions.hearth.disabled", "The hearth rests. (Ember charcoal is disabled in the config.)");

        add("feastcraft_expeditions.gate.not_in_base", "The gate only opens from the base.");
        add("feastcraft_expeditions.deploy.searching", "Plotting a drop point...");
        add("feastcraft_expeditions.deploy.busy", "A deployment is already underway.");
        add("feastcraft_expeditions.deploy.failed", "No safe drop point was found. Try the gate again.");

        add("feastcraft_expeditions.exfil.called", "Extraction called. Be in the zone when the countdown ends.");
        add("feastcraft_expeditions.exfil.not_raiding", "Only a raider can call extraction.");
        add("feastcraft_expeditions.exfil.spent", "This beacon is spent.");
        add("feastcraft_expeditions.exfil.refused", "The beacon does not answer.");
        add("feastcraft_expeditions.exfil.countdown", "Extraction in %s");
        add("feastcraft_expeditions.exfil.held", "Extraction held");

        add("commands.feastcraft_expeditions.beacon.reset", "Reset the exfil beacon at %s");
        add("commands.feastcraft_expeditions.beacon.nearest", "Nearest unspent exfil beacon: around %s, %s (%s blocks)");
        add("commands.feastcraft_expeditions.beacon.none", "No unspent exfil beacon within range");
        add("commands.feastcraft_expeditions.deploy", "Deploying %s");
        add("commands.feastcraft_expeditions.deploy.busy", "%s is already deploying or on a raid");
        add("commands.feastcraft_expeditions.recall", "Recalled %s to the base");
        add("commands.feastcraft_expeditions.raid.info", "%s is raiding: dropped at %s, %s seconds ago");
        add("commands.feastcraft_expeditions.raid.none", "%s is at the base");
        add("commands.feastcraft_expeditions.fuel", "Hearth fuel at %s: %s (%s ticks)");
        add("commands.feastcraft_expeditions.fuel.no_hearth", "There is no hearth at %s");
    }
}
