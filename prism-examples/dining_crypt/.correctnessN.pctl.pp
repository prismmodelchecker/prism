#const N#
// Correctness for the case where the master pays
(pay=0) => P>=1 [ F (#& j=1:N#s#j#=1#end#) & func(mod,(#+ j=1:N#agree#j##end#),2)=#func(mod,N,2)# ]

// Correctness for the case where a cryptographer pays
(pay>0) => P>=1 [ F (#& j=1:N#s#j#=1#end#) & func(mod,(#+ j=1:N#agree#j##end#),2)=#func(mod,N+1,2)# ]
