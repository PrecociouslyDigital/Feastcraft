"""`uv run python -m thaumcompat biomes --instance <dir>`, and `install --instance <dir>`."""

import argparse
import shutil
from collections import Counter
from pathlib import Path

from . import biomes, budget, derive, emit, export, raw, tables

HERE = Path(__file__).resolve().parent.parent
DATAPACK = HERE / "datapack"
EXPORT = HERE.parent.parent / "build" / "datapack-export"
REPORT = HERE / "subjects.tsv"
INSTALLED_AS = Path("config/openloader/data/feastcraft_thaumcraft_compat")


def aspects(export_dir: Path) -> None:
    loaded = export.load(export_dir)
    hand = tables.hand(HERE / "aspects.toml", loaded)
    tag_parents = tables.tag_parents(HERE / "tags.toml", loaded)
    resolved = derive.resolve(loaded, hand, tag_parents, {})
    uses = raw.uses(loaded)
    raw_materials = raw.raw_materials(loaded, resolved)
    kept = budget.kept(loaded, resolved, raw_materials, uses)
    blocks = budget.block_items(loaded)
    to_ask = [s for s in raw_materials if s.id not in blocks or s.id in kept]
    entries = emit.entries(loaded, resolved, kept)
    emit.write(DATAPACK, entries)
    emit.report(REPORT, loaded, resolved, set(to_ask), kept)
    steps = Counter(type(r).__name__ for r in resolved.values())
    print(f"{len(entries)} entries ({', '.join(f'{n} {k}' for k, n in sorted(steps.items()))}); "
          f"{len(to_ask)} raw materials to ask about; {len(kept)} block items in the budget")


def main() -> None:
    parser = argparse.ArgumentParser(prog="thaumcompat")
    commands = parser.add_subparsers(dest="command", required=True)
    biomes_command = commands.add_parser("biomes", help="cut Thaumcraft's biomes into Terralith's overworld")
    biomes_command.add_argument("--instance", type=Path, required=True)
    aspects_command = commands.add_parser("aspects", help="work out aspects and write them into the datapack")
    aspects_command.add_argument("--export", type=Path, default=EXPORT)
    install_command = commands.add_parser("install", help="copy the datapack into an instance's Open Loader folder")
    install_command.add_argument("--instance", type=Path, required=True)
    args = parser.parse_args()

    if args.command == "biomes":
        print(f"{biomes.write(args.instance, DATAPACK)} biome entries in the overworld")
    elif args.command == "aspects":
        aspects(args.export)
    elif args.command == "install":
        target = args.instance / INSTALLED_AS
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(DATAPACK, target)
        print(f"installed to {target}")


main()
