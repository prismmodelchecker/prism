for k in 14 16 18 ;do
	for n in 20 1500; do
		../../bin/prism ../models/zeroconf/zeroconf.nm ../models/zeroconf/zeroconf.pctl -s -const N=$n,K=$k,reset=false,err=.1 -epsilon 1e-8 -prop 2 -gs >../logs/Zeroconf/zeroconf_k_$k\_n_$n\_gs.log

		../../bin/prism ../models/zeroconf/zeroconf.nm ../models/zeroconf/zeroconf.pctl -s -const N=$n,K=$k,reset=false,err=.1 -prop 2 -modpoliter -epsilon 1e-8 >../logs/Zeroconf/zeroconf_k_$k\_n_$n\_mpi.log

		../../bin/prism ../models/zeroconf/zeroconf.nm ../models/zeroconf/zeroconf.pctl -s -const N=$n,K=$k,reset=false,err=.1 -prop 2 -topological -modpoliter -epsilon 1e-8 >../logs/Zeroconf/zeroconf_k_$k\_n_$n\_SCCmpi.log

		../../bin/prism ../models/zeroconf/zeroconf.nm ../models/zeroconf/zeroconf.pctl -s -const N=$n,K=$k,reset=false,err=.1 -prop 2 -improvedmodpoliter -epsilon 1e-8 >../logs/Zeroconf/zeroconf_k_$k\_n_$n\_Improvedmpi.log

		../../bin/prism ../models/zeroconf/zeroconf.nm ../models/zeroconf/zeroconf.pctl -s -const N=$n,K=$k,reset=false,err=.1 -prop 2 -topological -improvedmodpoliter -epsilon 1e-8 >../logs/Zeroconf/zeroconf_k_$k\_n_$n\_ImprovedSCCmpi.log

		../../bin/prism ../models/zeroconf/zeroconf.nm ../models/zeroconf/zeroconf.pctl -s -const N=$n,K=$k,reset=false,err=.1 -prop 2 -ii -epsilon 1e-10 >../logs/Zeroconf/zeroconf_k_$k\_n_$n\_intervaliteration.log

	done
done
