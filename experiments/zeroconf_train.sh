for K in `seq 1 6` ; do

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/zeroconf.pctl -prop 4 -s -ii:upper=1e+12 -improvedbk -maxiters 900500 -const K=$K,N=3000,reset=false,err=.12 > training/zeroconf/zeroconf\_$K\.log

done

