"""What a node is and what it gives. Where it sits is layout's business."""

from dataclasses import dataclass, field
from enum import Enum
from typing import Any

from .stats import Stat

NAMESPACE = "feastcraft_skill_tree"

Json = dict[str, Any]


def _number(amount: float) -> str:
    return f"{amount:g}"


@dataclass(frozen=True)
class Bonus:
    """A stat, in the units its tooltip shows: `Bonus(BLADE_DAMAGE, 4)` is +4% Blade Damage."""

    stat: Stat
    amount: float

    def text(self) -> str:
        info = self.stat.info
        sign = "+" if self.amount >= 0 else "-"
        return f"{sign}{_number(abs(self.amount))}{info.grant.suffix} {info.name}"


@dataclass(frozen=True)
class Multiply:
    """A `multiply_total` modifier: `Multiply(ARMOR, 50)` makes armor count half again."""

    stat: Stat
    percent: float

    def text(self) -> str:
        return f"{self.stat.info.name} ×{_number(1 + self.percent / 100)}"


@dataclass(frozen=True)
class OriginPower:
    """One of Origins' own powers, named as Origins names it."""

    path: str

    @property
    def id(self) -> str:
        return f"origins:{self.path}"


@dataclass(frozen=True)
class CustomPower:
    """A power this datapack defines. `body` is its Origins JSON, without name or description."""

    name: str
    summary: str
    body: Json = field(compare=False, hash=False)

    @property
    def id(self) -> str:
        return f"{NAMESPACE}:{self.name}"

    def text(self) -> str:
        return self.summary


class DamageKind(Enum):
    """Damage no attribute resists. Each becomes its own `modify_damage_taken` power."""

    EXPLOSION = ("explosion", "Explosion", {"type": "origins:in_tag", "tag": "minecraft:is_explosion"}, None)
    # Vanilla's is_fire tag covers lava too.
    FIRE = ("fire", "Fire and Lava", {"type": "origins:in_tag", "tag": "minecraft:is_fire"}, None)
    WITHER = ("wither", "Wither", {"type": "origins:name", "name": "wither"}, None)
    POISON = (
        "poison",
        "Poison",
        {"type": "origins:name", "name": "magic"},
        {"type": "origins:status_effect", "effect": "minecraft:poison"},
    )

    @property
    def key(self) -> str:
        key: str = self.value[0]
        return key

    @property
    def label(self) -> str:
        label: str = self.value[1]
        return label

    @property
    def damage_condition(self) -> Json:
        condition: Json = self.value[2]
        return condition

    @property
    def holder_condition(self) -> Json | None:
        condition: Json | None = self.value[3]
        return condition


@dataclass(frozen=True)
class Resist:
    """Less damage of some kinds. Emitted as powers named after the node, so nodes stack."""

    kinds: tuple[DamageKind, ...]
    percent: float

    def text(self) -> str:
        labels = [k.label for k in self.kinds]
        listed = labels[0] if len(labels) == 1 else ", ".join(labels[:-1]) + " and " + labels[-1]
        return f"-{_number(self.percent)}% {listed} Damage Taken"


class Slot(Enum):
    """A Curios slot, how many of it players have without the tree, and its name.

    A slot with items is one this datapack adds, and only those items fit it.
    """

    # Curios and Botania give two rings between them.
    RING = ("ring", 2, "Ring", ())
    CHARM = ("charm", 1, "Charm", ())
    HEAD = ("head", 1, "Head", ())
    MANA_TABLET = ("mana_tablet", 0, "Mana Tablet", ("botania:mana_tablet",))

    @property
    def key(self) -> str:
        key: str = self.value[0]
        return key

    @property
    def base(self) -> int:
        base: int = self.value[1]
        return base

    @property
    def label(self) -> str:
        label: str = self.value[2]
        return label

    @property
    def items(self) -> tuple[str, ...]:
        items: tuple[str, ...] = self.value[3]
        return items


@dataclass(frozen=True)
class ExtraSlot:
    """One more of a Curios slot. Nodes giving the same slot stack."""

    slot: Slot

    def text(self) -> str:
        return f"+1 {self.slot.label} Slot"


@dataclass(frozen=True)
class ManaTrickle:
    """Mana each second into the Botania mana items a player carries, from feastcraft_skill_rewards."""

    mana_per_second: int

    def text(self) -> str:
        return f"+{self.mana_per_second} Mana per Second into Carried Mana Items"


Reward = Bonus | Multiply | OriginPower | CustomPower | Resist | ExtraSlot | ManaTrickle


@dataclass(frozen=True)
class Start:
    title: str = "Start"
    icon: str = "minecraft:cake"


@dataclass(frozen=True)
class Travel:
    title: str
    rewards: tuple[Reward, ...]
    icon: str


@dataclass(frozen=True)
class Small:
    title: str
    rewards: tuple[Reward, ...]
    icon: str


@dataclass(frozen=True)
class Notable:
    title: str
    rewards: tuple[Reward, ...]
    icon: str


@dataclass(frozen=True)
class Keystone:
    title: str
    gain: str
    cost: str
    rewards: tuple[Reward, ...]
    icon: str


@dataclass(frozen=True)
class Origin:
    """An origin as a keystone. Every origin keystone excludes every other."""

    title: str
    gain: str
    cost: str
    powers: tuple[OriginPower, ...]
    icon: str


Content = Start | Travel | Small | Notable | Keystone | Origin


def powers(*paths: str) -> tuple[OriginPower, ...]:
    return tuple(OriginPower(p) for p in paths)
