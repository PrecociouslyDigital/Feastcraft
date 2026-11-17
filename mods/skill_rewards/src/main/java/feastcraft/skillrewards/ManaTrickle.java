package feastcraft.skillrewards;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import vazkii.botania.api.mana.ManaItemHandler;

/** Mana each second into the mana items a player carries, for each unlocked skill with this reward. */
final class ManaTrickle implements Reward {
    private static final int TICKS_PER_SECOND = 20;
    // Pufferfish makes one per skill definition, and drops them all on reload.
    private static final Set<ManaTrickle> LIVE = ConcurrentHashMap.newKeySet();
    // dispatchMana needs a non-empty stack as the mana's source; any item will do.
    private static final ItemStack SOURCE = new ItemStack(Items.GLASS_BOTTLE);

    private final int manaPerSecond;
    private final Map<UUID, Integer> unlocked = new ConcurrentHashMap<>();

    private ManaTrickle(int manaPerSecond) {
        this.manaPerSecond = manaPerSecond;
    }

    static Result<ManaTrickle, Problem> create(RewardConfigContext context) {
        return context.getData()
                .andThen(JsonElement::getAsObject)
                .andThen(data -> data.getInt("mana_per_second"))
                .mapSuccess(manaPerSecond -> {
                    ManaTrickle trickle = new ManaTrickle(manaPerSecond);
                    LIVE.add(trickle);
                    return trickle;
                });
    }

    @Override
    public void update(RewardUpdateContext context) {
        UUID player = context.getPlayer().getUUID();
        if (context.getCount() > 0) {
            unlocked.put(player, context.getCount());
        } else {
            unlocked.remove(player);
        }
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        LIVE.remove(this);
    }

    static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER || event.player.tickCount % TICKS_PER_SECOND != 0) {
            return;
        }
        UUID player = event.player.getUUID();
        int mana = LIVE.stream().mapToInt(trickle -> trickle.manaPerSecond * trickle.unlocked.getOrDefault(player, 0)).sum();
        if (mana > 0) {
            ManaItemHandler.instance().dispatchMana(SOURCE, event.player, mana, true);
        }
    }
}
