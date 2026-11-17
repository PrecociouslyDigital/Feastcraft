"""`uv run python -m thaumcompat biomes --instance <dir>`, and `install --instance <dir>`."""

import argparse
import shutil
from pathlib import Path

from . import biomes

HERE = Path(__file__).resolve().parent.parent
DATAPACK = HERE / "datapack"
INSTALLED_AS = Path("config/openloader/data/feastcraft_thaumcraft_compat")


def main() -> None:
    parser = argparse.ArgumentParser(prog="thaumcompat")
    commands = parser.add_subparsers(dest="command", required=True)
    biomes_command = commands.add_parser("biomes", help="cut Thaumcraft's biomes into Terralith's overworld")
    biomes_command.add_argument("--instance", type=Path, required=True)
    install_command = commands.add_parser("install", help="copy the datapack into an instance's Open Loader folder")
    install_command.add_argument("--instance", type=Path, required=True)
    args = parser.parse_args()

    if args.command == "biomes":
        print(f"{biomes.write(args.instance, DATAPACK)} biome entries in the overworld")
    elif args.command == "install":
        target = args.instance / INSTALLED_AS
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(DATAPACK, target)
        print(f"installed to {target}")


main()
