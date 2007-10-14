#const N#
const int k;

Pmin=? [ F (#& j=1:N#s#j#=1#end#) & (#+ j=1:N# #func(floor,func(pow,2,N-j))#*agree#j# #end# = k) {"init"&pay>0}{min} ]
Pmax=? [ F (#& j=1:N#s#j#=1#end#) & (#+ j=1:N# #func(floor,func(pow,2,N-j))#*agree#j# #end# = k) {"init"&pay>0}{max} ]
