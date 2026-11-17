package feastcraft.skillrewards;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.puffish.skillsmod.api.SkillsAPI;

@Mod(SkillRewards.MOD_ID)
public final class SkillRewards {
    public static final String MOD_ID = "feastcraft_skill_rewards";

    public SkillRewards() {
        SkillsAPI.registerReward(id("mana_trickle"), ManaTrickle::create);
        MinecraftForge.EVENT_BUS.addListener(ManaTrickle::onPlayerTick);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
