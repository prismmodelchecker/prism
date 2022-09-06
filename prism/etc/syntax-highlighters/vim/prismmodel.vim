" Vim syntax file
" Language: PRISM model files
" Latest Revision: 20 October 2011

if exists("b:current_syntax")
  finish
endif

syn keyword prismStructureKeyword ctmc dtmc mdp smg module endmodule formula nondeterministic probabilistic pta stochastic invariant endinvariant rewards endrewards init endinit system endsystem player endplayer
syn keyword prismBool true false
syn keyword prismVariableType bool clock const double global int rate label filter func
syn keyword prismFunction max min

syn region prismString start='"' end='"'

syn match prismOp "[&|<>=!+\-*/:?]"
syn match prismNumber "[0-9][0-9]*"
syn match prismVariableName "[_a-zA-Z][_a-zA-Z0-9]*"
syn match prismComment "//.*$"
syn match prismArrow "->"
syn match prismActionEmpty "\[\]"
syn match prismAction "\[[a-zA-Z][a-zA-Z0-9]*\]"

" A, , C  E, F, G, I, X, Pmax, Pmin, P prob,  Rmax, Rmin, R, S, U, W.

hi def link prismStructureKeyword Keyword
hi def link prismVariableType Type
hi def link prismVariableName Identifier
hi def link prismNumber Number
hi def link prismBool Boolean
hi def link prismComment Comment
hi def link prismString String
hi def link prismArrow Operator
hi def link prismOp Operator
hi def link prismActionEmpty Special
hi def link prismAction Special
