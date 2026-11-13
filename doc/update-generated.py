#!/usr/bin/env python3
"""Update the generated parts of the manual's Markdown from PRISM itself.

A generated part sits between two HTML comments (invisible on GitHub and on the website):

    <!-- generated: NAME (by doc/update-generated.py; do not edit) -->
    ...
    <!-- end generated -->

and everything between them is replaced. NAME says what goes there:

    keywords   PRISM's reserved keywords (prism -keywords)
    switches   PRISM's command-line switches, grouped as in prism -help (prism -helpjson)

Usage: doc/update-generated.py [path to the prism script]   (default: prism/bin/prism, from the repo root)
Run it after building PRISM, and commit the result. With --check, it changes nothing,
but lists the files that are out of date and exits with status 1 if there are any.
"""

import json
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


# Section titles (and anchors) for the groups of switches in prism -help, in sentence case;
# a group missing from here gets a title made from its name (a warning is printed)
GROUPS = {
    None: ("General", "general"),
    "IMPORTS": ("Imports", "imports"),
    "EXPORTS": ("Exports", "exports"),
    "EXPORT OPTIONS": ("Export options", "export-options"),
    "ENGINES/METHODS": ("Engines and methods", "engines"),
    "MODEL CONSTRUCTION OPTIONS": ("Model construction", "construction"),
    "SOLUTION METHODS (LINEAR EQUATIONS)": ("Solution methods (linear equations)", "linear-equations"),
    "SOLUTION METHODS (MDPS)": ("Solution methods (MDPs)", "mdp-methods"),
    "SOLUTION METHOD SETTINGS": ("Solution method settings", "solution-settings"),
    "MODEL CHECKING OPTIONS": ("Model checking", "model-checking"),
    "MULTI-OBJECTIVE MODEL CHECKING": ("Multi-objective model checking", "multi-objective"),
    "OUTPUT OPTIONS": ("Output", "output"),
    "SPARSE/HYBRID/MTBDD OPTIONS": ("Sparse, hybrid and MTBDD engines", "symbolic"),
    "PARAMETRIC MODEL CHECKING OPTIONS": ("Parametric model checking", "parametric"),
    "FAST ADAPTIVE UNIFORMISATION (FAU) OPTIONS": ("Fast adaptive uniformisation (FAU)", "fau"),
    "SIMULATION OPTIONS": ("Simulation", "simulation"),
}


def md_escape(text):
    """Escape text so that Markdown shows it as it is (e.g. <file>, [default: 1], a*b)."""
    return re.sub(r"([\\`*_\[\]<>|])", r"\\\1", text)


def md_details(lines):
    """Convert the text of prism -help <switch> into Markdown: prose, " * " lists and " " example lines."""
    out, prose, items, code = [], [], [], []

    def flush():
        if prose:
            out.append("\n".join(md_escape(l) for l in prose) + "\n")
        if items:
            out.append("\n".join(items) + "\n")
        if code:
            out.append("```\n" + "\n".join(code) + "\n```\n")
        prose.clear(), items.clear(), code.clear()

    for line in lines:
        if not line.strip():
            flush()
        elif line.startswith(" * "):
            if prose or code:
                flush()
            m = re.match(r"^(\S+)( \(=[^)]*\))? - (.*)$", line[3:].strip())
            if m:
                extra = f" ({md_escape(m.group(2).strip()[1:-1])})" if m.group(2) else ""
                items.append(f"- `{m.group(1)}`{extra}: {md_escape(m.group(3))}")
            else:
                items.append("- " + md_escape(line[3:].strip()))
        elif line.startswith(" "):
            if prose or items:
                flush()
            code.append(line[1:])
        elif line.startswith("Valid options: "):
            if items or code:
                flush()
            opts = line[len("Valid options: "):].split(", ")
            prose.append("Valid options: " + ", ".join(opts) + ".")
        else:
            if items or code:
                flush()
            prose.append(line)
    flush()
    return "\n".join(out)


def switches(prism):
    out = subprocess.run([prism, "-helpjson"], capture_output=True, text=True, check=True).stdout
    entries = json.loads(out[out.index("["):])
    used = set()
    sections = []
    group, lines = object(), None
    for e in entries:
        if e["hidden"]:
            continue
        if e["group"] != group:
            group = e["group"]
            title, anchor = GROUPS.get(group, (None, None))
            if title is None:
                print(f"warning: no title for switch group {group!r}", file=sys.stderr)
                title = group.capitalize()
                anchor = re.sub(r"[^a-z0-9]+", "-", group.lower()).strip("-")
            used.add(anchor)
            lines = []
            sections.append((title, anchor, lines))
        name = f"`-{e['name']}{(' ' if e['arg'] and not e['arg'].startswith('[:') else '')}{e['arg']}`"
        if e["aliases"]:
            name += " (or " + ", ".join(f"`-{a}`" for a in e["aliases"]) + ")"
        anchors = ""
        if not e.get("repeat"):
            for a in [e["name"]] + e["aliases"]:
                if a in used:
                    sys.exit(f"switch reference: anchor '{a}' used twice")
                used.add(a)
                anchors += f'<a id="{a}"></a>'
        item = f"- {anchors}{name}: {md_escape(e['text'])}\n"
        details = e["details"].split("\n")
        if details and details[0].strip() == e["text"].strip():
            details = details[1:]
        if any(l.strip() for l in details) and not e.get("repeat"):
            body = md_details(details)
            item += "\n" + "".join(("  " + l if l else "") + "\n" for l in body.rstrip("\n").split("\n")) + "\n"
        lines.append(item)
    text = []
    for title, anchor, lines in sections:
        text.append(f'<a id="{anchor}"></a>\n## {title}\n\n' + "".join(lines).rstrip("\n") + "\n")
    return "\n".join(text)


def main():
    args = [a for a in sys.argv[1:] if a != "--check"]
    check = "--check" in sys.argv[1:]
    prism = args[0] if args else str(ROOT / "prism" / "bin" / "prism")
    generators = {"keywords": lambda: keywords(prism), "switches": lambda: switches(prism)}
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
