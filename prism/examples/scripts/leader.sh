for l in 7 8; do
for k in 25 75 150 300; do
	../../bin/prism ../models/leader/asynchronous/leader$l.nm ../models/leader/asynchronous/leader.pctl -s -prop 3 -const K=$k > ../logs/leader/leader$l\_K_$k\.log

	../../bin/prism ../models/leader/asynchronous/leader$l.nm ../models/leader/asynchronous/leader.pctl -s -prop 3 -const K=$k -improvedbounded > ../logs/leader/leader$l\_K_$k\_improvedbounded.log

done

done
