"""Extra Curios slots. Pufferfish reruns an unlock command on every start, so these set a count."""

from ..model import Command

# Slots players have without the tree: Curios and Botania give two rings between them, Botania gives the charm.
BASE_SLOTS = {"ring": 2, "charm": 1}


def _one_more(slot: str) -> Command:
    base = BASE_SLOTS[slot]
    return Command(
        unlock=f"curios set {slot} @s {base + 1}",
        lock=f"curios set {slot} @s {base}",
        summary=f"+1 {slot.capitalize()} Slot",
    )


RING_SLOT = _one_more("ring")
CHARM_SLOT = _one_more("charm")
