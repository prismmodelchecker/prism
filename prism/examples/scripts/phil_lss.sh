for K in 10 20 30 35 40 45 50 55 60 65 70 75 80; do
  for l in 25 75 150 300; do
    ../../bin/prism ../models/phil_lss/phil_lss3.nm ../models/phil_lss/phil_lss3.pctl -prop 2 -const K=$K,L=$l -s > ../logs/phil_lss/phl_K_$K\_L_$l\.log 
    ../../bin/prism ../models/phil_lss/phil_lss3.nm ../models/phil_lss/phil_lss3.pctl -prop 2 -const K=$K,L=$l -s -improvedbounded > ../logs/phil_lss/phl_K_$K\_L_$l\_improvedbounded.log
  done
done
