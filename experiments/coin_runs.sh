for K in  30 32 34 36 38 40; do
up=$((48*K*K+72*K+36))
mx=$((161614/1000*K*K+8631/100*K+292))

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -ii:upper=$up -asynchupper -maxiters 9000300 -const K=$K -javamaxmem 4500m > runs/coin/coin4_$K\_steps_max_regr.log

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -improvedbk -maxiters $mx -const K=$K  -epsilon 1e-10 > runs/coin/coin4_$K\_vi_steps_max_limited_iters.log

#../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -javamaxmem 4500m -ex -ii -const K=$K -maxiters 95034600 > coin/coin4_$K\_ex.log

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -ii:upper=1.2e+12  -const K=$K -maxiters 9000300 -backward > runs/coin/coin4_$K\_steps_max_bk.log

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 -improvedbk > runs/coin/coin4_$K\_steps_max_improved.log

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 -improvedmpi > runs/coin/coin4_$K\_steps_max_improved_mpi.log

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 -asynchii > runs/coin/coin4_$K\_steps_max_asynch.log

#../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 > coin/coin4_$K\_ii.log

done
