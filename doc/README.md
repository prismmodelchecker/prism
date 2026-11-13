# PRISM documentation

`manual/` is the source of the PRISM user manual, in Markdown. It is designed to be read primarily from
its generated HTML, published at <https://www.prismmodelchecker.org/manual/> and included within PRISM
distributions in the `../manual/` folder, but also from the Markdown itself (notably on GitHub).
So a page should read properly as plain Markdown: there are no build-time includes or template variables,
and anything the website adds must be invisible on GitHub rather than broken.

## Layout

- **Sections:** each section of the manual is a folder (`ThePRISMLanguage/`) with a contents page, `README.md`:
  a `# Title`, an introduction to the section, then the section's pages, in order, as `- [Title](Page.md)` items,
  each optionally followed by a short description (`- [Strategies](Strategies.md): generating and exporting strategies`),
  which the website shows as a tooltip in the menu; the first paragraph of the introduction is the section's tooltip.
  GitHub shows it below the folder's files; the website publishes it at `/manual/ThePRISMLanguage/`,
  and the section's heading in the menu links to it. An indented item linking to `Page.md#anchor` adds an extra
  menu entry for part of a page. Only list items that start with a link to a `.md` file count as pages,
  and if the page has a `## Contents` heading, only those under it (up to the next heading),
  so lists of links elsewhere on the page, such as in the introduction, are left alone.
- **Contents:** `manual/README.md` is the manual's front page. It lists the sections (`- [Title](Folder/README.md)`),
  in order. The website builds its menu, and previous/next links, from this and the sections' contents pages,
  so add new pages there. Every page belongs to a section (the build warns about any that don't).
- **One file per page:** `Section/Page.md`, starting with a `# Title` line. It is published at `/manual/Section/Page/`.
- **Images and example files** go in `manual/files/` and are linked relatively (`../files/x.png`).
  Every image needs alt text describing what it shows: `![A plot of ... against ...](../files/x.png)`.
- **Example models and properties** that readers may want to download go in `manual/files/` as real files
  (`.prism`, `.props`), and are included in a page with a link alone in its paragraph, titled `"include"`:
  `[mutex.prism](../files/mutex.prism "include")`. The website shows the file's contents, highlighted,
  with a download link below; GitHub shows just the link. So the file is the only copy: don't also paste
  it into a code block. Short fragments that aren't whole files stay as ```` ```prism ```` blocks.

## Writing pages

- **Links between pages** are relative: `[filters](../PropertySpecification/Filters.md)`, or `Filters.md`
  within a section. These work on GitHub and are converted to website links when the site is built.
- **Anchors:** headings get ids automatically, as on GitHub (`## Reward structures` becomes `#reward-structures`).
  Where other pages or outside links point at a heading, give it a fixed anchor that won't change if the heading
  is reworded: put `<a id="name"></a>` on the line before it. (Not `{#name}` after the heading: GitHub would show it.)
- **Citations:** `[[KNP11](../Reference/References.md#KNP11)]`. `Reference/References.md` lists every cited paper, by key,
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

- **Generated parts:** some lists come from PRISM itself, such as the keywords in `Reference/PRISMLanguageSyntax.md` and the switches in `Reference/CommandLineSwitches.md`.
  They sit between `<!-- generated: NAME ... -->` and `<!-- end generated -->` comments and must not be edited by hand:
  after building PRISM, run `doc/update-generated.py` (from the repo root) and commit the result;
  `doc/update-generated.py --check` reports any that are out of date (the regression tests on GitHub run it after building PRISM).

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
