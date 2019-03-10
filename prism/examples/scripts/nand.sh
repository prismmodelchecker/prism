for k in  5 6; do
	for n in 50 60; do
		../../bin/prism ../models/nand/nand.pm ../models/nand/nand.pctl -s -const K=$k,N=$n -jacobi -nocompact -epsilon 1e-8 > ../logs/nand/nand_K_$k\_N_$n\_jacobi.log
		../../bin/prism ../models/nand/nand.pm ../models/nand/nand.pctl -s -const K=$k,N=$n -gs -nocompact -epsilon 1e-8 > ../logs/nand/nand_K_$k\_N_$n\_gs.log
		../../bin/prism ../models/nand/nand.pm ../models/nand/nand.pctl -s -const K=$k,N=$n -improvedgs -nocompact -epsilon 1e-8 > ../logs/nand/nand_K_$k\_N_$n\_improved_gs.log

		../../bin/prism ../models/nand/nand.pm ../models/nand/nand.pctl -s -const K=$k,N=$n -ii -nocompact -epsilon 1e-10 > ../logs/nand/nand_K_$k\_N_$n\_intervaliteration.log

	done
done
