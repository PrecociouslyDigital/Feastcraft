"""Thaumcraft 4 notables, run through TC4's own /thaumcraft command.

TC4 tells the player in chat each time, so these run rarely.
"""

from ..model import Notable
from . import power

TICKS_PER_MINUTE = 1200
WARP_MINUTES = 10
WARP_REMOVED = 2
NOTES_MINUTES = 30


def _every(minutes: int, command: str) -> dict[str, object]:
    return {"interval": minutes * TICKS_PER_MINUTE, "entity_action": {"type": "origins:execute_command", "command": command}}


CLEAR_MIND = Notable(
    "Clear Mind",
    (
        power(
            "clear_mind",
            f"Every {WARP_MINUTES} minutes, {WARP_REMOVED} temporary Warp fades",
            "action_over_time",
            **_every(WARP_MINUTES, f"thaumcraft warp @s add -{WARP_REMOVED} TEMP"),
        ),
    ),
    "thaumcraft:item_sanity_soap",
)
INITIATES_NOTES = Notable(
    "Initiate's Notes",
    (
        power(
            "initiates_notes",
            f"Every {NOTES_MINUTES} minutes, +1 research point of every aspect",
            "action_over_time",
            **_every(NOTES_MINUTES, "thaumcraft aspect @s all 1"),
        ),
    ),
    "thaumcraft:item_thaumonomicon",
)
