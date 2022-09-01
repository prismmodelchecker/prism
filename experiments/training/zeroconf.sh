for K in `seq 1 6` ; do

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1e+12 -improvedbk -maxiters 900500 -const K=$K,N=3000,reset=false,err=.12 > zeroconf/zeroconf\_$K\.log

done

