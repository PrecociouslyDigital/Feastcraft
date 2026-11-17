"""`uv run python -m recipes [--export <dir>]`, and `install --instance <dir>`."""

import argparse
import json
import shutil
from collections import Counter
from pathlib import Path

from . import bridges

HERE = Path(__file__).resolve().parent.parent
DATAPACK = HERE / "datapack"
GENERATED = DATAPACK / "data" / "feastcraft" / "recipes"
EXPORT = HERE.parent.parent / "build" / "datapack-export"
INSTALLED_AS = Path("config/openloader/data/feastcraft_recipes")


def write(made: list[bridges.Bridge]) -> None:
    for folder in ("casting", "hammering"):
        shutil.rmtree(GENERATED / folder, ignore_errors=True)
    for bridge in made:
        target = GENERATED / f"{bridge.path}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(bridge.recipe, indent=2) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser(prog="recipes")
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

    made, skipped = bridges.build(args.export)
    write(made)
    print(", ".join(f"{n} {folder}" for folder, n in Counter(b.path.split("/")[0] for b in made).items()))
    for reason, n in Counter(s.reason for s in skipped).most_common():
        print(f"skipped {n}: {reason}")


if __name__ == "__main__":
    main()
