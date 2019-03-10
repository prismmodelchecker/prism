for m in 4 6; do
  for n in 2048 4096 8192; do

	../../bin/prism ../models/brp/brp.pm  ../models/brp/brp.pctl -s -prop 3 -maxiters 50000000 -nocompact -jacobi -const N=$n,MAX=$m -epsilon 1e-8 > ../logs/brp/brp_N_$n\_MAX_$m\_jacobi.log

	../../bin/prism ../models/brp/brp.pm  ../models/brp/brp.pctl -s -prop 3 -maxiters 50000000 -nocompact -gs -const N=$n,MAX=$m -epsilon 1e-8 > ../logs/brp/brp_N_$n\_MAX_$m\_gs.log

	../../bin/prism ../models/brp/brp.pm  ../models/brp/brp.pctl -s -prop 3 -maxiters 50000000 -nocompact -improvedgs -const N=$n,MAX=$m -epsilon 1e-8 > ../logs/brp/brp_N_$n\_MAX_$m\_improved_gs.log

	../../bin/prism ../models/brp/brp.pm  ../models/brp/brp.pctl -s -prop 3 -maxiters 50000000 -nocompact -ii -const N=$n,MAX=$m -epsilon 1e-10 > ../logs/brp/brp_N_$n\_MAX_$m\_interval_iteration.log
  done
done

