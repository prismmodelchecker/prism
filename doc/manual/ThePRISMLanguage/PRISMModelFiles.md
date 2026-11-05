# PRISM Model Files

Files containing model descriptions written in the PRISM language
can contain any amount of white space (spaces, tabs, new lines, etc.),
all of which is ignored when the file is parsed by the tool.
Comments can also be used included in files in the style of the C programming language,
by preceding them with the characters `//`.
This is illustrated by the PRISM language examples from earlier in this section.

We recommend that the `.prism` extension is used for PRISM model files.
Historically (when the tool supported fewer types of model),
different extensions were often used for each model type:
`.nm` for MDPs or PTAs, `.pm` for DTMCs and `.sm` for CTMCs.
