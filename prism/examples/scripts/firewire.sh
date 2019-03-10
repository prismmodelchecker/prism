for dd in 7000 9500; do
for dl in 36 60 64 68 72 128; do

../../bin/prism ../models/firewire/abst/deadline.nm  ../models/firewire/abst/deadline.pctl -s -const delay=$dl,deadline=$dd,fast=.1 -javamaxmem 2350m -modpoliter -topological -epsilon 1e-8 > ../logs/firewire/firewire_abst_delay_$dd\_deadline_$dl\_scc_mpi.log

../../bin/prism ../models/firewire/abst/deadline.nm  ../models/firewire/abst/deadline.pctl -s -const delay=$dl,deadline=$dd,fast=.1 -javamaxmem 2350m -modpoliter  -epsilon 1e-8 > ../logs/firewire/firewire_abst_delay_$dd\_deadline_$dl\_mpi.log

../../bin/prism ../models/firewire/abst/deadline.nm  ../models/firewire/abst/deadline.pctl -s -const delay=$dl,deadline=$dd,fast=.1 -javamaxmem 2350m -gs -epsilon 1e-8 > ../logs/firewire/firewire_abst_delay_$dd\_deadline_$dl\_gs.log

../../bin/prism ../models/firewire/abst/deadline.nm  ../models/firewire/abst/deadline.pctl -s -const delay=$dl,deadline=$dd,fast=.1 -javamaxmem 2350m -improvedmodpoliter -epsilon 1e-8 > ../logs/firewire/firewire_abst_delay_$dd\_deadline_$dl\_improved_mpi.log
done
done
