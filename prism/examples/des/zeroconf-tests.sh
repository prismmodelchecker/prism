# min/max probability using fresh/used ip address

#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des 'used_use:*' 'true' -max >! logs/forwards.zeroconf.log
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline25.des 'used_use:*:before' 'true' -max >! logs/forwards.zeroconf.25.log
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline50.des 'used_use:*:before' 'true' -max >! logs/forwards.zeroconf.50.log
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline100.des 'used_use:*:before' 'true' -max >! logs/forwards.zeroconf.100.log

#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des 'used_use:*' 'true' -max -opt >! logs/forwards.zeroconf.log.opt
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline25.des 'used_use:*:before' 'true' -max -opt >! logs/forwards.zeroconf.25.log.opt
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline50.des 'used_use:*:before' 'true' -max -opt >! logs/forwards.zeroconf.50.log.opt
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline100.des 'used_use:*:before' 'true' -max -opt >! logs/forwards.zeroconf.100.log.opt

#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des 'used_use:*' 'true' -max -opt -nopre >! logs/forwards.zeroconf.log.nopre.opt
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline25.des 'used_use:*:before' 'true' -max -opt -nopre >! logs/forwards.zeroconf.25.log.nopre.opt
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline50.des 'used_use:*:before' 'true' -max -opt -nopre >! logs/forwards.zeroconf.50.log.nopre.opt
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline100.des 'used_use:*:before' 'true' -max -opt -nopre >! logs/forwards.zeroconf.100.log.nopre.opt

../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des 'used_use:*' 'true' -max -opt -nopre -refine=all >! logs/forwards.zeroconf.log.nopre.opt.all
../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline10.des 'used_use:*:before' 'true' -max -opt -nopre  -refine=all >! logs/forwards.zeroconf.10.log.nopre.opt.all
../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline15.des 'used_use:*:before' 'true' -max -opt -nopre  -refine=all >! logs/forwards.zeroconf.15.log.nopre.opt.all
../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline20.des 'used_use:*:before' 'true' -max -opt -nopre  -refine=all >! logs/forwards.zeroconf.20.log.nopre.opt.all
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline50.des 'used_use:*:before' 'true' -max -opt -nopre  -refine=all >! logs/forwards.zeroconf.50.log.nopre.opt.all
#../../bin/ptamc zeroconf/des/sender4.des zeroconf/des/environment.des zeroconf/des/deadline100.des 'used_use:*:before' 'true' -max -opt -nopre  -refine=all >! logs/forwards.zeroconf.100.log.nopre.opt.all
