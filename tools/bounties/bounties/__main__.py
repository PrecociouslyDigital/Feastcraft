"""`uv run python -m bounties [--export <dir>]`, and `install --instance <dir>`."""

import argparse
import shutil
from collections import Counter
from pathlib import Path

from . import market
from .rules import Rarity
from .select import ember_share

DATAPACK = market.HERE / "datapack"
REPORT = market.HERE / "report.tsv"
EXPORT = market.HERE.parent.parent / "build" / "datapack-export"
INSTALLED_AS = Path("config/openloader/data/feastcraft_bounties")


def main() -> None:
    parser = argparse.ArgumentParser(prog="bounties")
    parser.add_argument("--export", type=Path, default=EXPORT)
    commands = parser.add_subparsers(dest="command")
    install = commands.add_parser("install", help="copy the datapack into an instance's Open Loader folder")
    install.add_argument("--instance", type=Path, required=True)
    args = parser.parse_args()

    if args.command == "install":
        target = args.instance / INSTALLED_AS
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(DATAPACK, target)
        print(f"installed to {target}")
        return

    built = market.build(args.export)
    market.write(built, DATAPACK, REPORT)
    objectives = Counter(entry.category for entry in built.objectives)
    rewards = Counter(entry.category for entry in built.rewards)
    print(f"objectives: {', '.join(f'{n} {c}' for c, n in objectives.items())}")
    print(f"rewards: {', '.join(f'{n} {c}' for c, n in rewards.items())}")
    print(f"Ember Charcoal is {ember_share(built.rewards, Rarity.COMMON):.1%} of reward weight; "
          f"{len(built.stock)} stock decrees and pools turned off")


if __name__ == "__main__":
    main()
