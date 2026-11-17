"""Builds the Feastcraft .mrpack from an ATLauncher instance, and a server directory from a .mrpack.

Run through scripts/modpack; see README.md.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import tomllib
import urllib.parse
import urllib.request
import zipfile
from dataclasses import dataclass
from fnmatch import fnmatch
from pathlib import Path, PurePosixPath

MODRINTH = "https://api.modrinth.com/v2"
FORGE_MAVEN = "https://maven.minecraftforge.net/net/minecraftforge/forge"
HEADERS = {"User-Agent": "mrpack-builder", "Content-Type": "application/json"}

# Instance folders whose files are looked up on Modrinth, so the pack can link to them.
CONTENT_DIRS = ("mods", "resourcepacks", "shaderpacks")
CLIENT_ONLY_DIRS = ("resourcepacks", "shaderpacks")
# Jars the repo builds. The pack takes these from the repo, never from the instance.
OWN_JARS = "feastcraft_*"
SKILL_TREE_PATH = "config/openloader/data/feastcraft_skill_tree"


@dataclass(frozen=True)
class Hosted:
    """A file on Modrinth, which the pack links to."""

    url: str
    sha1: str
    sha512: str
    size: int
    client_only: bool


def fetch_json(url: str, body: object | None = None) -> object:
    data = None if body is None else json.dumps(body).encode()
    with urllib.request.urlopen(urllib.request.Request(url, data, HEADERS)) as response:
        return json.load(response)


def download(url: str, target: Path) -> None:
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS)) as response, target.open("wb") as file:
        shutil.copyfileobj(response, file)


def digest(path: Path, algorithm: str) -> str:
    with path.open("rb") as file:
        return hashlib.file_digest(file, algorithm).hexdigest()


def files_under(root: Path, relative: str) -> list[str]:
    start = root / relative
    found = [start] if start.is_file() else [p for p in start.rglob("*") if p.is_file()]
    return sorted(p.relative_to(root).as_posix() for p in found)


def find_on_modrinth(instance: Path, paths: list[str]) -> dict[str, Hosted]:
    by_sha1 = {digest(instance / path, "sha1"): path for path in paths}
    versions = fetch_json(f"{MODRINTH}/version_files", {"hashes": list(by_sha1), "algorithm": "sha1"})
    project_ids = sorted({version["project_id"] for version in versions.values()})
    server_side = {}
    for start in range(0, len(project_ids), 100):
        ids = urllib.parse.quote(json.dumps(project_ids[start : start + 100]))
        for project in fetch_json(f"{MODRINTH}/projects?ids={ids}"):
            server_side[project["id"]] = project["server_side"]

    hosted = {}
    for sha1, version in versions.items():
        file = next(f for f in version["files"] if f["hashes"]["sha1"] == sha1)
        hosted[by_sha1[sha1]] = Hosted(
            url=file["url"],
            sha1=sha1,
            sha512=file["hashes"]["sha512"],
            size=file["size"],
            client_only=server_side[version["project_id"]] == "unsupported",
        )
    return hosted


def build(args: argparse.Namespace) -> None:
    instance: Path = args.instance
    config = tomllib.loads((Path(__file__).parent / "pack.toml").read_text())
    launcher = json.loads((instance / "instance.json").read_text())
    loader = launcher["launcher"]["loaderVersion"]
    if (launcher["id"], loader["type"], loader["version"]) != (args.minecraft, "Forge", args.forge):
        raise SystemExit(
            f"{instance.name} runs {launcher['id']} with {loader['type']} {loader['version']}, "
            f"but gradle.properties builds for {args.minecraft} with Forge {args.forge}"
        )

    content = [
        f"{directory}/{entry.name}"
        for directory in CONTENT_DIRS
        for entry in sorted((instance / directory).iterdir())
        if entry.is_file() and not entry.name.startswith(".") and not fnmatch(entry.name, OWN_JARS)
    ]
    hosted = find_on_modrinth(instance, content)

    def named_in(path: str, patterns: list[str]) -> bool:
        return any(fnmatch(PurePosixPath(path).name, pattern) for pattern in patterns)

    def client_only(path: str) -> bool:
        return (
            path.split("/")[0] in CLIENT_ONLY_DIRS
            or (path in hosted and hosted[path].client_only and not named_in(path, config["server_too"]))
            or named_in(path, config["client_only"])
        )

    index_files = [
        {
            "path": path,
            "hashes": {"sha1": file.sha1, "sha512": file.sha512},
            "env": {"client": "required", "server": "unsupported" if client_only(path) else "required"},
            "downloads": [file.url],
            "fileSize": file.size,
        }
        for path, file in sorted(hosted.items())
    ]

    # Archive path -> file on disk, for everything the pack carries itself.
    bundled: dict[str, Path] = {}
    carried = [path for path in content if path not in hosted]
    for relative in config["overrides"]:
        carried += files_under(instance, relative)
    for path in carried:
        if not any(fnmatch(path, pattern) for pattern in config["exclude"]):
            bundled[f"{'client-overrides' if client_only(path) else 'overrides'}/{path}"] = instance / path
    for jar in args.jar:
        bundled[f"overrides/mods/{jar.name}"] = jar
    for path in files_under(args.skill_tree, "."):
        bundled[f"overrides/{SKILL_TREE_PATH}/{path}"] = args.skill_tree / path

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": config["version"],
        "name": config["name"],
        "files": index_files,
        "dependencies": {"minecraft": args.minecraft, "forge": args.forge},
    }
    out: Path = args.out / f"{config['name']}-{config['version']}.mrpack"
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as pack:
        pack.writestr("modrinth.index.json", json.dumps(index, indent=2))
        for name, source in sorted(bundled.items()):
            pack.write(source, name)

    bundled_mods = sorted(name for name in bundled if PurePosixPath(name).parent.name == "mods")
    client_side = sum(1 for file in index_files if file["env"]["server"] == "unsupported")
    print(f"{out} ({out.stat().st_size / 1e6:.0f} MB)")
    print(f"  {len(index_files)} files linked from Modrinth, {client_side} of them client-only")
    print(f"  {len(bundled_mods)} mods bundled because Modrinth doesn't have them:")
    for name in bundled_mods:
        print(f"    {name}")


def server(args: argparse.Namespace) -> None:
    out: Path = args.out
    out.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.pack) as pack:
        index = json.loads(pack.read("modrinth.index.json"))

        # A mod the pack has dropped must not stay behind.
        shutil.rmtree(out / "mods", ignore_errors=True)

        for file in index["files"]:
            if file["env"]["server"] == "unsupported":
                continue
            target = out / file["path"]
            if not target.resolve().is_relative_to(out.resolve()):
                raise SystemExit(f"{args.pack.name} lists a file outside the server: {file['path']}")
            target.parent.mkdir(parents=True, exist_ok=True)
            local = args.local / file["path"] if args.local else None
            if local and local.is_file():
                shutil.copyfile(local, target)
            else:
                print(f"downloading {file['path']}")
                download(file["downloads"][0], target)
            if digest(target, "sha512") != file["hashes"]["sha512"]:
                raise SystemExit(f"{file['path']} is not the file the pack lists")

        for layer in ("overrides", "server-overrides"):
            for name in pack.namelist():
                if name.startswith(f"{layer}/") and not name.endswith("/"):
                    target = out / name.removeprefix(f"{layer}/")
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(pack.read(name))

    forge = f"{index['dependencies']['minecraft']}-{index['dependencies']['forge']}"
    installed = out / "libraries/net/minecraftforge/forge" / forge
    if not installed.is_dir():
        installer = out / f"forge-{forge}-installer.jar"
        download(f"{FORGE_MAVEN}/{forge}/{installer.name}", installer)
        subprocess.run(["java", "-jar", installer.name, "--installServer"], cwd=out, check=True, stdout=subprocess.DEVNULL)
        installer.unlink()
        (out / f"{installer.name}.log").unlink(missing_ok=True)

    mods = sum(1 for path in (out / "mods").iterdir() if path.is_file())
    print(f"{out}: {mods} mods on Forge {forge}")
    if not (out / "eula.txt").exists():
        print("Before the first start, accept Minecraft's EULA by writing eula=true to eula.txt.")
    print("Start it with ./run.sh nogui, on Java 17.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(required=True)

    build_command = commands.add_parser("build")
    build_command.add_argument("--instance", type=Path, required=True)
    build_command.add_argument("--out", type=Path, required=True)
    build_command.add_argument("--minecraft", required=True)
    build_command.add_argument("--forge", required=True)
    build_command.add_argument("--skill-tree", type=Path, required=True)
    build_command.add_argument("--jar", type=Path, action="append", default=[])
    build_command.set_defaults(run=build)

    server_command = commands.add_parser("server")
    server_command.add_argument("--pack", type=Path, required=True)
    server_command.add_argument("--out", type=Path, required=True)
    server_command.add_argument("--local", type=Path)
    server_command.set_defaults(run=server)

    args = parser.parse_args()
    args.run(args)


if __name__ == "__main__":
    main()
