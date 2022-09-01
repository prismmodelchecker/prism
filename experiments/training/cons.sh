for k in `seq 1 8`; do

../../bin/prism ../../examples/mdps/consensus/coin4.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii -const K=$k -maxiters 90005000 -improvedbk > cons/coin4_$k\_stepsmax_impbk.log

../../bin/prism ../../examples/mdps/consensus/coin5.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii -const K=$k -maxiters 90005000 -improvedbk > cons/coin5_$k\_stepsmax_impbk.log

done

