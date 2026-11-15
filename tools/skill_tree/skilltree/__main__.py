"""Generates the datapack and preview: `uv run python -m skilltree [--install <instance dir>]`."""

import argparse
import shutil
from pathlib import Path

from . import emit, preview
from .tree import tree

HERE = Path(__file__).resolve().parent.parent
DATAPACK = HERE / "datapack"
PREVIEW = HERE / "build" / "preview.svg"
INSTALLED_AS = Path("config/openloader/data/feastcraft_skill_tree")


def main() -> None:
    parser = argparse.ArgumentParser(prog="skilltree")
    parser.add_argument("--install", type=Path, metavar="INSTANCE", help="copy the datapack into this instance's Open Loader folder")
    args = parser.parse_args()

    built = tree()
    emit.write(built, DATAPACK)
    preview.write(built, PREVIEW)
    print(f"{len(built.nodes)} nodes -> {DATAPACK.relative_to(HERE)}, preview at {PREVIEW.relative_to(HERE)}")

    if args.install is not None:
        target = args.install / INSTALLED_AS
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(DATAPACK, target)
        print(f"installed to {target}")


main()
