# PRISM documentation

`manual/` is the source of the PRISM user manual, in Markdown. It is designed to be read primarily from
its generated HTML, published at <https://www.prismmodelchecker.org/manual/> and included within PRISM
distributions in the `../manual/` folder, but also from the Markdown itself (notably on GitHub).
So a page should read properly as plain Markdown: there are no build-time includes or template variables,
and anything the website adds must be invisible on GitHub rather than broken.

## Layout

- **Sections:** each section of the manual is a folder (`ThePRISMLanguage/`) with a contents page, `README.md`:
  a `# Title`, a sentence or two of introduction, then the section's pages, in order, as `- [Title](Page.md)` items.
  GitHub shows it below the folder's files; the website publishes it at `/manual/ThePRISMLanguage/`,
  and the section's heading in the menu links to it. An indented item linking to `Page.md#anchor` adds an extra
  menu entry for part of a page. Only list items that start with a link to a `.md` file count as pages.
- **Contents:** `manual/README.md` lists the sections (`- [Title](Folder/README.md)`) and any pages outside
  a section (`- [Introduction](Main/Introduction.md)`), in order. The website builds its menu, and
  previous/next links, from this and the sections' contents pages, so add new pages there.
- **One file per page:** `Section/Page.md`, starting with a `# Title` line. It is published at `/manual/Section/Page/`.
- **Images and example files** go in `manual/files/` and are linked relatively (`../files/x.png`).
  Every image needs alt text describing what it shows: `![A plot of ... against ...](../files/x.png)`.

## Writing pages

- **Links between pages** are relative: `[filters](../PropertySpecification/Filters.md)`, or `Filters.md`
  within a section. These work on GitHub and are converted to website links when the site is built.
- **Anchors:** headings get ids automatically, as on GitHub (`## Reward structures` becomes `#reward-structures`).
  Where other pages or outside links point at a heading, give it a fixed anchor that won't change if the heading
  is reworded: put `<a id="name"></a>` on the line before it. (Not `{#name}` after the heading: GitHub would show it.)
- **Citations:** `[[KNP11](../Main/References.md#KNP11)]`. `Main/References.md` lists every cited paper, by key,
  and is edited by hand: when adding a citation, add an entry there too (the website build warns if the two don't match).
- **Code:** fenced blocks with a language: `prism` (models and properties, highlighted on the website), `shell`, `text`, `xml`.
  In `shell` blocks, write commands as they are and start each line of output with `#`
  (the website shows a `$` before each command, and the output without the `#`); start a script with `#!`.
  Inline code (keywords, properties, switches, file names) is plain backticks, not syntax highlighted.
- **Maths:** LaTeX between dollar signs, which GitHub and the website both show as maths: `$\alpha$` inline,
  and `$$ ... $$` on lines of their own for a displayed formula (e.g. `$$N = \frac{\ln(2/\delta)}{2\varepsilon^2}$$`).
  Use it for formulas and symbols, not for PRISM syntax (properties, expressions), which goes in backticks.
- **Versions:** say which version a feature appeared in ("Since PRISM 4.10, ...") rather than "now",
  "currently" or "in newer versions", which date badly. Don't hard-code the current version
  (e.g. in file names, write `prism-X.Y-linux64-x86`).
  The website publishes the manual from master, so changes appear straight away, ahead of the release they describe.

## Renaming, moving and merging pages

Pages are linked from papers, slides and mailing lists, so old URLs must keep working.
When a page is renamed, moved or merged into another, add its old name to `manual/redirects/main.json`:
`"OldGroup/OldPage": "NewGroup/NewPage.md"`, or `"NewGroup/NewPage.md#anchor"` if it became part of a page.
Keys are page names as in URLs (the old PmWiki names are there too); values are files in `manual/`,
which the website build checks exist. Variants of the manual (PRISM-games) add their own file to `redirects/`.

## Variants of the manual

PRISM-games has its own version of this manual: it merges this repository regularly, adds its own sections and pages
(new folders and files, plus a line in `manual/README.md`), and edits shared pages directly, like any other file.

## Other outputs

The website also publishes the whole manual on one page (`/manual/all/`). Distributions include a self-contained
copy of that page in a top-level `manual/` folder (not kept in git): `make doc` (from `prism/`) builds it from this
folder, using the website's build code
([prism-www](https://github.com/prismmodelchecker/prism-www)): a checkout of it next to this repo's, or `PRISM_WWW=<path>`,
or else it fetches what it needs (this needs Node.js; see the Makefile).
The release workflow does this, so each release, or any other build, gets the manual for its own version.
