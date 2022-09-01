for n in 3; do
for t in `seq 1 20` ; do
ttm=$((100*t))

up=$((110721*ttm+339138))
mx=$((10*ttm+45))

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -ii:upper=$up -asynchupper -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max_upper_regression.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -improvedbk -maxiters $mx -epsilon 1e-10 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max_vi_limitediters.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -ii:upper=1.2e+12 -backward -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max_bk.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -ii:upper=1.2e+12 -improvedbk -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max_improved.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -ii:upper=1.2e+12 -asynchii -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max_asynch.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/cost_max.pctl -s -ii:upper=$up -asynchii -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_cost_max_asynch_regression.log

up=$((152*ttm+12070))
mx=$((9*ttm+23))

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -ii:upper=$up -asynchupper -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max_upper_regression.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -improvedbk -maxiters $mx -epsilon 1e-10 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max_vi_limitediters.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -ii:upper=1.2e+12 -backward -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max_bk.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -ii:upper=1.2e+12 -improvedbk -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max_improved.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -ii:upper=1.2e+12 -asynchii -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max_asynch.log

../../bin/prism ../../examples/mdps/wlan/wlan$n\.nm ../../examples/mdps/wlan/time_max.pctl -s -ii:upper=$up -asynchii -maxiters 900500 -const COL=5 -const TRANS_TIME_MAX=$ttm > wlan/wlan$n\_$ttm\_time_max_asynch_regression.log

done
done
