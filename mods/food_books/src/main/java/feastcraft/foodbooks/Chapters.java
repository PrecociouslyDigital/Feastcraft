package feastcraft.foodbooks;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** The two books as chapters of one: Carrot's, everything ever eaten, then Onion's, the current diet. */
public final class Chapters {
    private Chapters() {
    }

    public static void openOnion(Player player) {
        team.creative.solonion.client.gui.screen.FoodBookScreen.open(player);
    }

    public static void openCarrotAtLastPage(Player player) {
        var carrot = new com.cazsius.solcarrot.client.gui.FoodBookScreen(player);
        Minecraft.getInstance().setScreen(carrot);
        // Carrot's screen class is final, so javac rejects a direct cast to the interface its mixin adds.
        carrot.switchToPage(((PageCount) (Object) carrot).feastcraft_food_books$pageCount() - 1);
    }

    /**
     * Both books' page buttons are unlabelled, so the narrator and the test harness cannot tell
     * them apart. Each book has only these two buttons, back on the left.
     */
    public static void labelPageButtons(Screen book) {
        List<AbstractButton> buttons = book.children().stream()
                .filter(AbstractButton.class::isInstance)
                .map(AbstractButton.class::cast)
                .sorted(Comparator.comparingInt(AbstractWidget::getX))
                .toList();
        if (buttons.size() == 2) {
            buttons.get(0).setMessage(Component.translatable("spectatorMenu.previous_page"));
            buttons.get(1).setMessage(Component.translatable("spectatorMenu.next_page"));
        }
    }
}
