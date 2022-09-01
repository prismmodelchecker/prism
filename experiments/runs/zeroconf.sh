for t in `seq 9 20` ; do

up=$((472/100*t+665/10))
mx=$((41/10*t+33))

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=$up -asynchupper -maxiters 900500 -const K=$t,N=3000,reset=false,err=.1 > zeroconf/zeroconf_K$t\_upper_regression.log

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -improvedbk -maxiters $mx -epsilon 1e-10 -const K=$t,N=3000,reset=false,err=.1 > zeroconf/zeroconf_K$t\_max_cost_vi_limitediters.log

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1.2e+12 -backward -maxiters 900500 -const K=$t,N=3000,reset=false,err=.1 > zeroconf/zeroconf_K$t\_max_cost_bk.log

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1.2e+12 -improvedbk -maxiters 900500 -const K=$t,N=3000,reset=false,err=.1 > zeroconf/zeroconf_K$t\_cost_max_improved.log

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1.2e+12 -asynchii -maxiters 900500 -const K=$t,N=3000,reset=false,err=.1 > zeroconf/zeroconf_K$t\_cost_max_asynch.log

../../bin/prism ../../examples/mdps/zeroconf/zeroconf.nm ../../examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=$up -asynchii -maxiters 900500 -const K=$t,N=3000,reset=false,err=.1 > zeroconf/zeroconf_K$t\_cost_max_asynch_regression.log


done
