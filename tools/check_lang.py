"""Release check for the language files.

Two things bite late: a key used in code that nobody translated, and a key that only exists in one
language. Both are cheap to catch here instead of in a crash report or a raw translation key in the
tooltip.
"""
import io
import json
import os
import re

JAVA_ROOT = "src/main/java"
LANG_DIR = "src/main/resources/assets/kalioupo_equipment/lang"

# keys built at runtime (mode names, suffixes) are checked as prefixes instead of literals
PREFIXES = (
    "trait.kalioupo_equipment.",
    "skill.kalioupo_equipment.",
)


def keys_used():
    used = set()
    prefix_hits = set()

    for root, _, files in os.walk(JAVA_ROOT):
        for name in files:
            if not name.endswith(".java"):
                continue

            text = io.open(os.path.join(root, name), encoding="utf-8").read()

            for match in re.finditer(r'translatable\(\s*"([^"]+)"', text):
                used.add(match.group(1))

            for match in re.finditer(r'"((?:trait|skill)\.kalioupo_equipment\.[^"]*)"\s*\+', text):
                prefix_hits.add(match.group(1))
            for match in re.finditer(r'\+ \(\?(?!:)', text):
                pass

    return used, prefix_hits


def load(name):
    with io.open(os.path.join(LANG_DIR, name), encoding="utf-8") as f:
        return json.load(f)


def missing_from(used, table):
    """A literal that is a prefix of real keys is a runtime-built key, so it counts as present."""
    return sorted(
        key for key in used
        if key not in table and not any(existing.startswith(key) for existing in table)
    )


def main():
    zh = load("zh_cn.json")
    en = load("en_us.json")
    used, prefixes = keys_used()

    missing_zh = missing_from(used, zh)
    missing_en = missing_from(used, en)

    only_zh = sorted(set(zh) - set(en))
    only_en = sorted(set(en) - set(zh))

    # runtime built keys: report any prefix that has no matching entries at all
    empty_prefix = sorted(p for p in prefixes if not any(k.startswith(p) for k in zh))

    print("used literals:", len(used))
    print("missing in zh_cn:", missing_zh or "none")
    print("missing in en_us:", missing_en or "none")
    print("only in zh_cn:", only_zh or "none")
    print("only in en_us:", only_en or "none")
    print("runtime prefixes with no keys:", empty_prefix or "none")
    print("key counts:", len(zh), len(en))


if __name__ == "__main__":
    main()
