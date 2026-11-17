import random
import unittest

from thaumcompat import questions
from thaumcompat.aspects import ASPECTS
from thaumcompat.export import Export, Food, Subject


def subjects(n: int, seed: int = 0) -> list[Subject]:
    rng = random.Random(seed)
    return [Subject("item", f"mod{rng.randrange(5)}:thing_{rng.randrange(10**6)}") for _ in range(n)]


def make_export(of: list[Subject]) -> Export:
    long_tags = {f"forge:a_long_tag/name_number_{i}": frozenset(s.id for s in of) for i in range(20)}
    return Export(
        items={s.id: Food(20, 1.2) for s in of},
        names={s.id: "A Fairly Long English Name For A Modded Item" for s in of},
        blocks={}, mobs=frozenset(), recipes=(), known={}, item_tags=long_tags, entity_tags={},
    )


class RequestTests(unittest.TestCase):
    def test_the_same_subjects_give_the_same_requests(self) -> None:
        of = subjects(50)
        first = [r.key for r in questions.requests(make_export(of), "jev", of)]
        again = [r.key for r in questions.requests(make_export(of), "jev", list(reversed(of)))]
        self.assertEqual(first, again)

    def test_adding_a_subject_changes_few_requests(self) -> None:
        changed = []
        for seed in range(200):
            of = subjects(60, seed)
            extra = subjects(1, seed + 1000)[0]
            changed.append(len(set(questions.groups(of + [extra])) - set(questions.groups(of))))
        self.assertLessEqual(sum(changed) / len(changed), 2.5)

    def test_a_full_request_fits_the_limits(self) -> None:
        of = subjects(questions.MOST_SUBJECTS)
        request = questions.request(make_export(of), "jev", tuple(of))
        self.assertLessEqual(len(request.body), questions.MAX_BYTES)
        self.assertLessEqual(2 * len(ASPECTS) * len(of), questions.MAX_OPTIONS)

    def test_answers_come_back_to_their_subjects(self) -> None:
        of = subjects(3)
        request = questions.request(make_export(of), "jev", tuple(of))
        answers = {f"{letter}__{aspect}": {"noul": 0.9 if letter == "b" and aspect == "metallum" else 0.1}
                   for letter in "abc" for aspect in ASPECTS}
        self.assertEqual(0.9, questions.probabilities(request, answers)[of[1]]["metallum"])


class AspectTests(unittest.TestCase):
    def test_keeps_at_most_four_likely_aspects_with_amounts_by_probability(self) -> None:
        p = dict.fromkeys(ASPECTS, 0.1) | {"metallum": 0.97, "terra": 0.8, "ignis": 0.6, "aqua": 0.55, "aer": 0.52}
        self.assertEqual({"metallum": 4, "terra": 3, "ignis": 1, "aqua": 1}, questions.aspects(p))

    def test_with_nothing_likely_keeps_the_best_guess(self) -> None:
        p = dict.fromkeys(ASPECTS, 0.1) | {"herba": 0.3}
        self.assertEqual({"herba": 1}, questions.aspects(p))
        self.assertTrue(questions.unsure(p))


if __name__ == "__main__":
    unittest.main()
