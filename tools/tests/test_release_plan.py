import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from release_plan import plan, version_code  # noqa: E402


class ReleasePlanTest(unittest.TestCase):
    TAGS = ["v0.1.1", "preview", "v0.1.2", "vbad"]
    APP = ["app/src/main/java/com/openjlpt/app/MainActivity.kt"]

    def test_default_is_a_patch_release(self):
        result = plan([], "", self.APP, self.TAGS)
        self.assertTrue(result["needed"])
        self.assertEqual("0.1.3", result["version"])
        self.assertEqual(103, result["version_code"])

    def test_labels_pick_the_bump(self):
        self.assertEqual("0.2.0", plan(["release:minor"], "", self.APP, self.TAGS)["version"])
        self.assertEqual("1.0.0", plan(["release:major", "bug"], "", self.APP, self.TAGS)["version"])
        self.assertFalse(plan(["release:skip"], "", self.APP, self.TAGS)["needed"])

    def test_explicit_version(self):
        self.assertEqual("0.5.0", plan([], "Notes\nRelease-Version: 0.5.0\n", self.APP, self.TAGS)["version"])
        self.assertEqual("0.1.3", plan([], "<!-- Release-Version: 9.9.9 -->", self.APP, self.TAGS)["version"])
        with self.assertRaises(ValueError):
            plan([], "Release-Version: 0.1.0", self.APP, self.TAGS)
        with self.assertRaises(ValueError):
            plan(["release:minor"], "Release-Version: 0.5.0", self.APP, self.TAGS)

    def test_docs_and_ci_changes_do_not_release(self):
        result = plan([], "", ["README.md", "docs/QUESTION_FORMAT.md", ".github/workflows/android.yml"], self.TAGS)
        self.assertFalse(result["needed"])
        self.assertTrue(plan([], "", ["README.md", "app/src/main/assets/questions/n5/grammar.json"], self.TAGS)["needed"])

    def test_forced_release_ignores_labels_and_paths(self):
        result = plan(["release:skip"], "", ["README.md"], self.TAGS, forced_bump="minor")
        self.assertEqual("0.2.0", result["version"])

    def test_first_release_without_tags(self):
        self.assertEqual("0.1.1", plan([], "", self.APP, [])["version"])

    def test_version_code_grows(self):
        self.assertLess(version_code((0, 1, 99)), version_code((0, 2, 0)))
        self.assertLess(version_code((0, 99, 0)), version_code((1, 0, 0)))


if __name__ == "__main__":
    unittest.main()
