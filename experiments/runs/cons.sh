for K in  30 32 34 36 38 40; do
up=$((48*K*K+72*K+36))
mx=$((161614/1000*K*K+8631/100*K+292))

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=$up -asynchupper -maxiters 9000300 -const K=$K -javamaxmem 4500m > coin/coin4_$K\_steps_max_regr.log

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -improvedbk -maxiters $mx -const K=$K  -epsilon 1e-10 > coin/coin4_$K\_vi_steps_max_limited_iters.log

#../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -javamaxmem 4500m -ex -ii -const K=$K -maxiters 95034600 > coin/coin4_$K\_ex.log

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=1.2e+12  -const K=$K -maxiters 9000300 -backward > coin/coin4_$K\_steps_max_bk.log

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 -improvedbk > coin/coin4_$K\_steps_max_improved.log

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 -improvedmpi > coin/coin4_$K\_steps_max_improved_mpi.log

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 -asynchii > coin/coin4_$K\_steps_max_asynch.log

#../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii:upper=1.2e+12 -const K=$K -maxiters 9000300 > coin/coin4_$K\_ii.log

done
