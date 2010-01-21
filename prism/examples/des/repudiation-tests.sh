# probability protocol terminates correctly with no errors
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des 'done:*' 'true' -min -opt >! logs/forwards.repudiation.honest.log.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline40.des '*:*:before' 'true' -min -opt >! logs/forwards.repudiation.honest.40.log.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline80.des '*:*:before' 'true' -min -opt >! logs/forwards.repudiation.honest.80.log.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline100.des '*:*:before' 'true' -min -opt >! logs/forwards.repudiation.honest.100.log.opt

#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des 'done:*' 'true' -min -opt -nopre >! logs/forwards.repudiation.honest.log.nopre.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline40.des '*:*:before' 'true' -min -opt -nopre >! logs/forwards.repudiation.honest.40.log.nopre.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline80.des '*:*:before' 'true' -min -opt -nopre >! logs/forwards.repudiation.honest.80.log.nopre.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline100.des '*:*:before' 'true' -min -opt -nopre >! logs/forwards.repudiation.honest.100.log.nopre.opt

../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des 'done:*' 'true' -min -opt -nopre >! logs/forwards.repudiation.honest.log.nopre.opt.all
../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline40.des '*:*:before' 'true' -min -opt -nopre -refine=all >! logs/forwards.repudiation.honest.40.log.nopre.opt.all
../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline80.des '*:*:before' 'true' -min -opt -nopre -refine=all >! logs/forwards.repudiation.honest.80.log.nopre.opt.all
../../bin/ptamc repudiation/des/originator.des repudiation/des/recipient.des repudiation/des/honest_deadline100.des '*:*:before' 'true' -min -opt -nopre -refine=all >! logs/forwards.repudiation.honest.100.log.nopre.opt.all

# probability protocol terminates and recipient has gained information
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des 'done_error:*' 'true' -max -opt  >! logs/forwards.repudiation.malicous.log.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline5.des  '*:*:before' 'true' -max -opt >! logs/forwards.repudiation.malicous.5.log.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline10.des '*:*:before' 'true' -max -opt >! logs/forwards.repudiation.malicous.10.log.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline20.des '*:*:before' 'true' -max -opt >! logs/forwards.repudiation.malicous.20.log.opt

#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des 'done_error:*' 'true' -max -opt -nopre  >! logs/forwards.repudiation.malicous.log.nopre.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline5.des  '*:*:before' 'true' -max -opt -nopre >! logs/forwards.repudiation.malicous.5.log.nopre.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline10.des '*:*:before' 'true' -max -opt -nopre >! logs/forwards.repudiation.malicous.10.log.nopre.opt
#../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline20.des '*:*:before' 'true' -max -opt -nopre >! logs/forwards.repudiation.malicous.20.log.nopre.opt

../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des 'done_error:*' 'true' -max -opt -nopre -refine=all >! logs/forwards.repudiation.malicous.log.nopre.opt.all
../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline5.des '*:*:before' 'true' -max -opt -nopre -refine=all >! logs/forwards.repudiation.malicous.5.log.nopre.opt.all
../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline10.des '*:*:before' 'true' -max -opt -nopre -refine=all >! logs/forwards.repudiation.malicous.10.log.nopre.opt.all
../../bin/ptamc repudiation/des/originator.des repudiation/des/malicous_recipient.des repudiation/des/malicous_deadline20.des '*:*:before' 'true' -max -opt -nopre -refine=all >! logs/forwards.repudiation.malicous.20.log.nopre.opt.all

