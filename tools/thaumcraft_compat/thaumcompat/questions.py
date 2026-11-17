"""Asks OpenRouter's decision models which aspects a raw material has, one yes/no question per aspect."""

from __future__ import annotations

import hashlib
import json
import shutil
from collections.abc import Iterable, Mapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal

from .aspects import ASPECTS
from packexport import Aspects, Export, Subject

Model = Literal["jev", "mammoth"]
MODEL_IDS: dict[Model, str] = {"jev": "typesafe/jev-1.14", "mammoth": "baseten/mammoth-1"}
# Jev slows down and gets worse past this many bytes; mammoth-1 can't read much more.
MAX_BYTES = 32000
MAX_OPTIONS = 512
MOST_SUBJECTS = 4
MOST_TAGS = 12
MOST_ASPECTS = 4
LETTERS = "abcdefgh"

ABOUT = (
    "Thaumcraft says what a thing is made of or stands for with aspects. Each subject is a Minecraft item "
    "or mob from a mod. Each question asks whether one subject has one aspect, and aspects says what each "
    "aspect covers. Most things have two to four aspects."
)


@dataclass(frozen=True)
class Request:
    """One call to the Decisions API. Its key is the hash of the body, and names the answer file."""

    model: Model
    subjects: tuple[Subject, ...]
    body: bytes

    @property
    def key(self) -> str:
        return hashlib.sha256(self.body).hexdigest()


def _tags(tags: list[str]) -> list[str]:
    common = ("forge", "c", "minecraft")
    return sorted(tags, key=lambda tag: (tag.split(":")[0] not in common, tag))[:MOST_TAGS]


def state(export: Export, subject: Subject) -> dict[str, Any]:
    out: dict[str, Any] = {"id": subject.id, "name": export.names.get(subject.id, ""), "tags": _tags(export.tags_of(subject))}
    if subject.kind == "entity":
        out["mob"] = True
    elif (food := export.items.get(subject.id)) is not None:
        out["food"] = {"hunger": food.hunger, "saturation": food.saturation}
    return out


def _question(letter: str, aspect: str) -> dict[str, Any]:
    return {
        "type": "noul",
        "instructions": f"Does {letter} have {aspect}?",
        "criteria": {"true": f"{letter} has {aspect}", "false": f"{letter} doesn't"},
    }


def request(export: Export, model: Model, subjects: tuple[Subject, ...]) -> Request:
    """One request asking every aspect of every subject. Subjects are named a, b, c, ... in the questions."""
    letters = dict(zip(LETTERS, subjects))
    body = {
        "model": MODEL_IDS[model],
        "state": {"about": ABOUT, "aspects": ASPECTS, **{letter: state(export, s) for letter, s in letters.items()}},
        "questions": {f"{letter}__{aspect}": _question(letter, aspect) for letter in letters for aspect in ASPECTS},
    }
    encoded = json.dumps(body, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()
    return Request(model, subjects, encoded)


def _boundary(subject: Subject) -> bool:
    return hashlib.sha256(f"{subject.kind} {subject.id}".encode()).digest()[0] % MOST_SUBJECTS == 0


def groups(subjects: Iterable[Subject]) -> list[tuple[Subject, ...]]:
    """Subjects in id order, split after a subject whose hash says so, or when a group is full.

    Splitting on the subjects themselves rather than on their position means adding or removing one
    changes only its own group and maybe the next, so the other requests keep their answers.
    """
    out: list[tuple[Subject, ...]] = []
    group: list[Subject] = []
    for subject in sorted(subjects, key=lambda s: (s.kind, s.id)):
        group.append(subject)
        if len(group) == MOST_SUBJECTS or _boundary(subject):
            out.append(tuple(group))
            group = []
    if group:
        out.append(tuple(group))
    return out


def requests(export: Export, model: Model, subjects: Iterable[Subject]) -> list[Request]:
    return [request(export, model, group) for group in groups(subjects)]


def probabilities(request: Request, answers: Mapping[str, Any]) -> dict[Subject, dict[str, float]]:
    """Each subject's probability of having each aspect."""
    return {
        subject: {aspect: float(answers[f"{letter}__{aspect}"]["noul"]) for aspect in ASPECTS}
        for letter, subject in zip(LETTERS, request.subjects)
    }


def aspects(probabilities: Mapping[str, float]) -> Aspects:
    """The most probable aspects over even odds, at most four, with amounts from 1 to 4 by probability.

    With none over even odds, the single most probable aspect at 1.
    """
    ranked = sorted(probabilities.items(), key=lambda pair: (-pair[1], pair[0]))
    likely = [(aspect, p) for aspect, p in ranked[:MOST_ASPECTS] if p >= 0.5] or ranked[:1]
    return dict(sorted((aspect, max(1, min(4, 1 + int((p - 0.5) / 0.125)))) for aspect, p in likely))


def unsure(probabilities: Mapping[str, float]) -> bool:
    """Whether to ask mammoth-1 as well: no aspect is clearly there. About one subject in ten."""
    return max(probabilities.values()) < 0.7


@dataclass(frozen=True)
class Store:
    decisions: Path
    pending: Path

    def answered(self) -> dict[str, Any]:
        if not self.decisions.is_dir():
            return {}
        return {path.stem: json.loads(path.read_text()) for path in self.decisions.glob("*.json")}

    def settle(self, waiting: list[Request], referenced: set[str]) -> None:
        """Make the pending folder hold exactly the requests still to send, and drop answers no request refers to.

        Which subjects mammoth-1 is asked about depends on Jev's answers, so nothing is dropped while
        Jev has requests waiting.
        """
        if self.pending.exists():
            shutil.rmtree(self.pending)
        self.pending.mkdir(parents=True)
        for request in waiting:
            (self.pending / f"{request.key}.json").write_bytes(request.body)
        if not any(request.model == "jev" for request in waiting):
            for path in self.decisions.glob("*.json"):
                if path.stem not in referenced:
                    path.unlink()
