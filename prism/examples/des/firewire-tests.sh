# abstract firewire model
# eventually a leader is elected

#../../bin/ptamc firewire/des/abs.des 'seldone' 'true' -min     >! logs/forwards.firewire.abs.infinity.log
#../../bin/ptamc firewire/des/abs500.des 'before' 'true' -min   >! logs/forwards.firewire.abs.500.log
#../../bin/ptamc firewire/des/abs1000.des 'before' 'true' -min  >! logs/forwards.firewire.abs.1000.log
#../../bin/ptamc firewire/des/abs2000.des 'before' 'true' -min  >! logs/forwards.firewire.abs.2000.log

#../../bin/ptamc firewire/des/abs.des 'seldone' 'true' -min -opt  >! logs/forwards.firewire.abs.infinity.log.opt
#../../bin/ptamc firewire/des/abs500.des 'before' 'true' -min -opt   >! logs/forwards.firewire.abs.500.log.opt
#../../bin/ptamc firewire/des/abs1000.des 'before' 'true' -min -opt  >! logs/forwards.firewire.abs.1000.log.opt
#../../bin/ptamc firewire/des/abs2000.des 'before' 'true' -min -opt  >! logs/forwards.firewire.abs.2000.log.opt

#../../bin/ptamc firewire/des/abs.des 'seldone' 'true' -min -opt -nopre >! logs/forwards.firewire.abs.infinity.log.nopre.opt
#../../bin/ptamc firewire/des/abs500.des 'before' 'true' -min -opt -nopre  >! logs/forwards.firewire.abs.500.log.nopre.opt
#../../bin/ptamc firewire/des/abs1000.des 'before' 'true' -min -opt -nopre >! logs/forwards.firewire.abs.1000.log.nopre.opt
#../../bin/ptamc firewire/des/abs2000.des 'before' 'true' -min -opt -nopre >! logs/forwards.firewire.abs.2000.log.nopre.opt

../../bin/ptamc firewire/des/abs.des 'seldone' 'true' -min    -opt -nopre -refine=all >! logs/forwards.firewire.abs.infinity.log.nopre.opt.all
../../bin/ptamc firewire/des/abs500.des 'before' 'true' -min  -opt -nopre -refine=all >! logs/forwards.firewire.abs.500.log.nopre.opt.all
../../bin/ptamc firewire/des/abs1000.des 'before' 'true' -min -opt -nopre -refine=all >! logs/forwards.firewire.abs.1000.log.nopre.opt.all
../../bin/ptamc firewire/des/abs2000.des 'before' 'true' -min -opt -nopre -refine=all >! logs/forwards.firewire.abs.2000.log.nopre.opt.all

##################################################################################################
# impl model of firewire
# eventually a leader is elected
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/done.des '*:*:*:*:done' 'true' -min -opt  >! logs/forwards.firewire.impl.infinity.log.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline300.des '*:*:*:*:before' 'true' -min -opt   >! logs/forwards.firewire.impl.300.log.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline400.des '*:*:*:*:before' 'true' -min -opt   >! logs/forwards.firewire.impl.400.log.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline500.des '*:*:*:*:before' 'true' -min -opt   >! logs/forwards.firewire.impl.500.log.opt

#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/done.des '*:*:*:*:done' 'true' -min -opt -nopre >! logs/forwards.firewire.impl.infinity.log.nopre.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline250.des '*:*:*:*:before' 'true' -min -opt -nopre  >! logs/forwards.firewire.impl.250.log.nopre.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline300.des '*:*:*:*:before' 'true' -min -opt -nopre  >! logs/forwards.firewire.impl.300.log.nopre.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline400.des '*:*:*:*:before' 'true' -min -opt -nopre  >! logs/forwards.firewire.impl.400.log.nopre.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline500.des '*:*:*:*:before' 'true' -min -opt -nopre  >! logs/forwards.firewire.impl.500.log.nopre.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline750.des '*:*:*:*:before' 'true' -min -opt -nopre  >! logs/forwards.firewire.impl.750.log.nopre.opt
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline1000.des '*:*:*:*:before' 'true' -min -opt -nopre  >! logs/forwards.firewire.impl.1000.log.nopre.opt

../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des 's7:s8:*:*,s8:s7:*:*' 'true' -min -opt -nopre -refine=all >! logs/forwards.firewire.impl.infinity.log.nopre.opt.all
../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline250.des '*:*:*:*:before' 'true' -min -opt -nopre -refine=all  >! logs/forwards.firewire.impl.250.log.nopre.opt.all
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline300.des '*:*:*:*:before' 'true' -min -opt -nopre -refine=all  >! logs/forwards.firewire.impl.300.log.nopre.opt.all
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline400.des '*:*:*:*:before' 'true' -min -opt -nopre -refine=all  >! logs/forwards.firewire.impl.400.log.nopre.opt.all
../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline500.des '*:*:*:*:before' 'true' -min -opt -nopre -refine=all  >! logs/forwards.firewire.impl.500.log.nopre.opt.all
../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline750.des '*:*:*:*:before' 'true' -min -opt -nopre -refine=all  >! logs/forwards.firewire.impl.750.log.nopre.opt.all
#../../bin/ptamc firewire/des/n1.des firewire/des/n2.des firewire/des/w12.des firewire/des/w21.des firewire/des/deadline1000.des '*:*:*:*:before' 'true' -min -opt -nopre -refine=all  >! logs/forwards.firewire.impl.1000.log.nopre.opt.all

