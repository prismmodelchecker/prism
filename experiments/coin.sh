for k in `seq 1 8`; do

../prism/bin/prism ../prism-examples/mdps/consensus/coin4.nm ../prism-examples/mdps/consensus/coin.pctl -prop 8 -s -ii -const K=$k -maxiters 90005000 -improvedbk > training/coin/coin4_$k\_stepsmax_impbk.log

#../prism/bin/prism ../prism-examples/mdps/consensus/coin5.nm ../../examples/mdps/consensus/steps_max.pctl -s -ii -const K=$k -maxiters 90005000 -improvedbk #> training/cons/coin5_$k\_stepsmax_impbk.log

done

