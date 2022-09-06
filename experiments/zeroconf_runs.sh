for t in `seq 9 20` ; do

up=$((473/100*t+675/10))
mx=$((41/10*t+33))

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=$up -asynchupper -maxiters 900500 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_upper_asynch_regression.log

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=$up -maxiters 900500 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_upper_regression.log

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -improvedbk -maxiters $mx -epsilon 1e-10 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_vi_limitediters.log

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1.2e+12 -backward -maxiters 900500 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_backward.log

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1.2e+12 -improvedbk -maxiters 900500 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_improved_backward.log

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=1.2e+12 -asynchii -maxiters 900500 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_asynch.log

../prism/bin/prism ../prism-examples/mdps/zeroconf/zeroconf.nm ../prism-examples/mdps/zeroconf/max_cost.pctl -s -ii:upper=$up -asynchii -maxiters 900500 -const K=$t,N=3000,reset=false,err=.12 > runs/zeroconf/zeroconf_K$t\_asynch_regression.log


done
