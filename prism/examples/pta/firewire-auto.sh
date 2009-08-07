# abstract firewire model

# eventually a leader is elected
../../bin/ptamc firewire/abs.des 'seldone' 'true' -min

# leader elected before/after a time bound

#../../bin/ptamc firewire/abs.des 'seldone' 'z<=2000' -min
../../bin/ptamc firewire/abs200.des 'before' 'true' -min -refine
../../bin/ptamc firewire/abs200.des 'after' 'true' -max -refine

#../../bin/ptamc firewire/abs.des 'seldone' 'z<=4000' -min
../../bin/ptamc firewire/abs400.des 'before' 'true' -min -refine
../../bin/ptamc firewire/abs400.des 'after' 'true' -max -refine

../../bin/ptamc firewire/abs600.des 'before' 'true' -min -refine
../../bin/ptamc firewire/abs600.des 'after' 'true' -max -refine

../../bin/ptamc firewire/abs800.des 'before' 'true' -min -refine
../../bin/ptamc firewire/abs800.des 'after' 'true' -max -refine

../../bin/ptamc firewire/abs1000.des 'before' 'true' -min -refine
../../bin/ptamc firewire/abs1000.des 'after' 'true' -max -refine

../../bin/ptamc firewire/abs2000.des 'before' 'true' -min -refine
../../bin/ptamc firewire/abs2000.des 'after' 'true' -max -refine


##################################################################################################
# impl model of firewire

# leader eventually elected
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/done.des '*:*:*:*:done' 'true' -min

# leader elected before/after a time bound

../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline200.des '*:*:*:*:before' 'true' -min
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline200.des '*:*:*:*:after' 'true' -max

../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline400.des '*:*:*:*:before' 'true' -min
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline400.des '*:*:*:*:after' 'true' -max

../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline600.des '*:*:*:*:before' 'true' -min
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline600.des '*:*:*:*:after' 'true' -max

../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline800.des '*:*:*:*:before' 'true' -min
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline800.des '*:*:*:*:after' 'true' -max

../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline1000.des '*:*:*:*:before' 'true' -min
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline1000.des '*:*:*:*:after' 'true' -max

../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline2000.des '*:*:*:*:before' 'true' -min
../../bin/ptamc firewire/n1.des firewire/n2.des firewire/w12.des firewire/w21.des firewire/deadline2000.des '*:*:*:*:after' 'true' -max

