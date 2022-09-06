for n in 3; do
for ttm in `seq 1 20` ; do

#../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -ii:upper=1e+12 -improvedbk -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -ii:upper=1e+12 -improvedbk -maxiters 900500 -const COL=3 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max.log

#../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/num_collisions.pctl -s -ii:upper=1e+12 -improvedbk -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_num_collisions.log

done
done
