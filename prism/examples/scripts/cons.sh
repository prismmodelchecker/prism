for k in 20 36; do
  ../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -gs -maxiters 25000000 -epsilon 1e-8> ../logs/consensus/coin4_k_$k\_gs.log

  ../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -modpoliter -maxiters 25000000 -epsilon 1e-8> ../logs/consensus/coin4_k_$k\_mpi.log

  ../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -modpoliter -topological -maxiters 25000000 -epsilon 1e-8 > ../logs/consensus/coin4_k_$k\_SCCmpi.log

  ../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -improvedmodpoliter -topological -maxiters 25000000 -epsilon 1e-8 > ../logs/consensus/coin4_k_$k\_ImprovedSCCmpi.log

  ../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -ii -maxiters 25000000 -epsilon 1e-10 > ../logs/consensus/coin4_k_$k\_Intervaliteration.log
done

for k in 15 32; do
  ../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -gs -maxiters 25000000 -epsilon 1e-8> ../logs/consensus/coin5_k_$k\_gs.log

  ../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -modpoliter -maxiters 25000000 -epsilon 1e-8> ../logs/consensus/coin5_k_$k\_mpi.log

  ../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -modpoliter -topological -maxiters 25000000 -epsilon 1e-8 > ../logs/consensus/coin5_k_$k\_SCCmpi.log

  ../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -improvedmodpoliter -topological -maxiters 25000000 -epsilon 1e-8 > ../logs/consensus/coin5_k_$k\_ImprovedSCCmpi.log

  ../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -ii -maxiters 25000000 -epsilon 1e-10 > ../logs/consensus/coin5_k_$k\_Intervaliteration.log
done

for k in  4 8; do
  ../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -gs -maxiters 25000000 -epsilon 1e-8> ../logs/consensus/coin6_k_$k\_gs.log

  ../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -modpoliter -maxiters 25000000 -epsilon 1e-8> ../logs/consensus/coin6_k_$k\_mpi.log

  ../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -modpoliter -topological -maxiters 25000000 -epsilon 1e-8 > ../logs/consensus/coin6_k_$k\_SCCmpi.log

  ../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -improvedmodpoliter -topological -maxiters 25000000 -epsilon 1e-8 > ../logs/consensus/coin6_k_$k\_ImprovedSCCmpi.log

  ../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -s -prop 4 -const K=$k -ii -maxiters 25000000 -epsilon 1e-10 > ../logs/consensus/coin6_k_$k\_Intervaliteration.log
done
