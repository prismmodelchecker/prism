
#../../bin/ptamc simple/iandc.des sr 'z<6'

#../../bin/ptamc simple/tcs.des l3 true

#../../bin/ptamc simple/gethin1.des l2 true

#../../bin/ptamc simple/gethin2.des l2 true

prism-explicit simple/formats.nm -pctl 'Pmax=? [ F s=3 ]' -aroptions refine=first,v
prism-explicit simple/formats.nm -pctl 'Rmin=? [ F s=1|s=3 ]'

