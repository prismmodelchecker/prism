" Vim syntax file
" Language: PRISM property files
" Latest Revision: 20 October 2011

if exists("b:current_syntax")
  finish
endif

syn match prismOp "[&|=<>]"
syn keyword prismFilterParam min max count sum avg first range forall exists state argmin argmax print
syn keyword prismStructureKeyword filter 
syn keyword prismBool true false
syn keyword prismVariableType bool  double int const
syn match prismVariableName "[_a-zA-Z][_a-zA-Z0-9]*"
syn match prismNumber "[0-9][0-9]*"
syn match prismComment "//.*$" contains=prismResult
syn match prismResult "RESULT.*$" contained
syn match prismStatePROperator "[PRS]\({[^\}]*}\|\)\(min\|max\|\)[=<>?]*[0-9]*" contains=prismNumberInOperator,prismString
syn match prismTemporalOperator "[ACEFGIXUW][=<>?]*[0-9]*" contains=prismNumberInOperator
syn match prismCoalition "<<[^>]*>>" contains=prismNumberInOperator
syn match prismNumberInOperator "[0-9]*" contained
syn region prismString start='"' end='"'



hi def link prismStructureKeyword Keyword
hi def link prismVariableType Type
hi def link prismVariableName Identifier
hi def link prismBool Boolean
hi def link prismNumberInOperator Number
hi def link prismNumber Number
hi def link prismComment Comment
hi def link prismString String
hi def link prismStatePROperator Operator
hi def link prismTemporalOperator Operator
hi def link prismTempOperator Operator
hi def link prismCoalition Operator
hi def link prismFilterParam Keyword
hi def link prismOp Operator
hi def link prismResult PreProc
