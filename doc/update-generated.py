#!/usr/bin/env python3
"""Update the generated parts of the manual's Markdown from PRISM itself.

A generated part sits between two HTML comments (invisible on GitHub and on the website):

    <!-- generated: NAME (by doc/update-generated.py; do not edit) -->
    ...
    <!-- end generated -->

and everything between them is replaced. NAME says what goes there:

    keywords   PRISM's reserved keywords (prism -keywords)

Usage: doc/update-generated.py [path to the prism script]   (default: prism/bin/prism, from the repo root)
Run it after building PRISM, and commit the result. With --check, it changes nothing,
but lists the files that are out of date and exits with status 1 if there are any.
"""

import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MANUAL = ROOT / "doc" / "manual"
REGION = re.compile(r"(<!-- generated: (\w+)[^>]*-->\n)(.*?)(<!-- end generated -->)", re.S)


def keywords(prism):
    out = subprocess.run([prism, "-keywords"], capture_output=True, text=True, check=True).stdout
    words = out.split(":", 1)[1].split()
    words.sort(key=lambda w: (w.lower(), w))
    return ", ".join(f"`{w}`" for w in words) + ".\n"


def main():
    args = [a for a in sys.argv[1:] if a != "--check"]
    check = "--check" in sys.argv[1:]
    prism = args[0] if args else str(ROOT / "prism" / "bin" / "prism")
    generators = {"keywords": lambda: keywords(prism)}
    cache = {}
    stale = []
    for path in sorted(MANUAL.rglob("*.md")):
        text = path.read_text()

        def replace(m):
            name = m.group(2)
            if name not in generators:
                sys.exit(f"{path}: unknown generated part '{name}'")
            if name not in cache:
                cache[name] = generators[name]()
            return m.group(1) + cache[name] + m.group(4)

        new = REGION.sub(replace, text)
        if new != text:
            stale.append(path.relative_to(ROOT))
            if not check:
                path.write_text(new)
    for p in stale:
        print(("out of date: " if check else "updated: ") + str(p))
    if check and stale:
        sys.exit(1)


if __name__ == "__main__":
    main()
