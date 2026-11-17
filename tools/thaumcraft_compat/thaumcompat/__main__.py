"""`uv run python -m thaumcompat biomes --instance <dir>`, and `install --instance <dir>`."""

import argparse
import dataclasses
import shutil
from collections import Counter
from pathlib import Path

import packexport

from . import biomes, budget, derive, emit, questions, raw, tables

HERE = Path(__file__).resolve().parent.parent
DATAPACK = HERE / "datapack"
REPO = HERE.parent.parent
EXPORT = REPO / "build" / "datapack-export"
REPORT = HERE / "subjects.tsv"
STORE = questions.Store(decisions=HERE / "decisions", pending=HERE / "build" / "pending")
INSTALLED_AS = Path("config/openloader/data/feastcraft_thaumcraft_compat")


def aspects(export_dir: Path) -> None:
    loaded = packexport.load(export_dir)
    hand = tables.hand(HERE / "aspects.toml", loaded)
    tag_parents = tables.tag_parents(HERE / "tags.toml", loaded)
    loaded = dataclasses.replace(loaded, known=loaded.known | tables.known(HERE / "aspects.toml", loaded))
    before_asking = derive.resolve(loaded, hand, tag_parents, {})
    uses = raw.uses(loaded)
    raw_materials = raw.raw_materials(loaded, before_asking)
    kept = budget.kept(loaded, before_asking, raw_materials, uses)
    blocks = budget.block_items(loaded)
    to_ask = [s for s in raw_materials if s.id not in blocks or s.id in kept]

    asked, waiting, referenced = ask(loaded, to_ask)
    resolved = derive.resolve(loaded, hand, tag_parents, asked)
    entries = emit.entries(loaded, resolved, kept)
    emit.write(DATAPACK, entries)
    emit.report(REPORT, loaded, resolved, set(to_ask), kept)
    STORE.settle(waiting, referenced)

    steps = Counter(type(r).__name__ if not isinstance(r, derive.Asked) else r.model for r in resolved.values())
    print(f"{len(entries)} entries ({', '.join(f'{n} {k}' for k, n in sorted(steps.items()))}); "
          f"{len(to_ask)} raw materials; {len(kept)} block items, {sum(len(blocks[i]) for i in kept)} blocks, in the budget")
    if waiting:
        tool = HERE.relative_to(REPO)
        print(f"{len(waiting)} requests wait in {STORE.pending.relative_to(HERE)}. From the repo root, send them with")
        print(f'  secretspec run --reason "Thaumcraft aspects" -- {tool}/decide.sh {tool}/decisions {tool}/build/pending')
        print("then run this again.")


def ask(loaded: packexport.Export, subjects: list[packexport.Subject]) -> tuple[dict[packexport.Subject, derive.Asked], list[questions.Request], set[str]]:
    """Answers so far, from mammoth-1 where Jev was unsure, and the requests still to send."""
    answered = STORE.answered()
    waiting: list[questions.Request] = []
    referenced: set[str] = set()

    def answers(model: questions.Model, of: list[packexport.Subject]) -> dict[packexport.Subject, dict[str, float]]:
        out: dict[packexport.Subject, dict[str, float]] = {}
        for request in questions.requests(loaded, model, of):
            referenced.add(request.key)
            if request.key in answered:
                out |= questions.probabilities(request, answered[request.key])
            else:
                waiting.append(request)
        return out

    jev = answers("jev", subjects)
    mammoth = answers("mammoth", [s for s, p in jev.items() if questions.unsure(p)])
    asked = {s: derive.Asked("jev", questions.aspects(p)) for s, p in jev.items()}
    asked |= {s: derive.Asked("mammoth", questions.aspects(p)) for s, p in mammoth.items()}
    return asked, waiting, referenced


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


if __name__ == "__main__":
    main()
