for K in 8 16 24 32 40 48; do
	for k in 25 75 150 300; do
		../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -s -prop 5 -const K=$K,k=$k > ../logs/consensus_bounded/coin_4_K_$K\_standard_bounded_$k\.log
		../../bin/prism ../models/consensus/coin4.nm ../models/consensus/coin.pctl -improvedbounded -s -prop 5 -const K=$K,k=$k > ../logs/consensus_bounded/coin_4_K_$K\_improved_bounded_$k\.log
	done
done

for K in 12 16 20 24 28 32; do
	for k in 25 75 150 300; do
		../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -s -prop 5 -const K=$K,k=$k > ../logs/consensus_bounded/coin_5_K_$K\_standard_bounded_$k\.log
		../../bin/prism ../models/consensus/coin5.nm ../models/consensus/coin.pctl -improvedbounded -s -prop 5 -const K=$K,k=$k > ../logs/consensus_bounded/coin_5_K_$K\_improved_bounded_$k\.log
	done
done

for K in 8 12 16 20 24 28 ; do
	for k in 25 75 150 300; do
		../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -s -prop 5 -const K=$K,k=$k > ../logs/consensus_bounded/coin_6_K_$K\_standard_bounded_$k\.log
		../../bin/prism ../models/consensus/coin6.nm ../models/consensus/coin.pctl -improvedbounded -s -prop 5 -const K=$K,k=$k > ../logs/consensus_bounded/coin_6_K_$K\_improved_bounded_$k\.log
	done
done
