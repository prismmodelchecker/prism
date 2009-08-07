# csma example from fmics paper

#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max  -opt  >! logs/forwards.csma2.c4.log.opt
#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max  -opt  >! logs/forwards.csma4.c4.log.opt
#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max  -opt  >! logs/forwards.csma2.c8.log.opt
#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max  -opt  >! logs/forwards.csma4.c8.log.opt

#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -nopre -opt  >! logs/forwards.csma2.c4.log.nopre.opt
#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -nopre -opt  >! logs/forwards.csma4.c4.log.nopre.opt
#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -nopre -opt  >! logs/forwards.csma2.c8.log.nopre.opt
#../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -nopre -opt  >! logs/forwards.csma4.c8.log.nopre.opt

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -nopre -opt -refine=all  >! logs/forwards.csma2.c4.log.nopre.opt.all
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -nopre -opt -refine=all  >! logs/forwards.csma4.c4.log.nopre.opt.all
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -nopre -opt -refine=all  >! logs/forwards.csma2.c8.log.nopre.opt.all
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -nopre -opt -refine=all  >! logs/forwards.csma4.c8.log.nopre.opt.all

################################################################################

#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des '*:finish:finish' 'true' -min -opt   >! ! logs/forwards.csma1.abst..log.nopre.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1000.des '*:*:*:before' 'true' -min -opt   >! logs/forwards.csma1.abst..1000.log.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -opt   >! logs/forwards.csma1.abst..2000.log.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline4000.des '*:*:*:before' 'true' -min -opt   >! logs/forwards.csma1.abst..4000.log.opt

#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des '*:finish:finish' 'true' -min -nopre -opt -refine=all   >! ! logs/forwards.csma1.abst..log.nopre.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1000.des '*:*:*:before' 'true' -min -nopre -opt   >! logs/forwards.csma1.abst..1000.log.nopre.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -nopre -opt   >! logs/forwards.csma1.abst..2000.log.nopre.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline3000.des '*:*:*:before' 'true' -min -nopre -opt   >! logs/forwards.csma1.abst..3000.log.nopre.opt
#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline4000.des '*:*:*:before' 'true' -min -nopre -opt   >! logs/forwards.csma1.abst..4000.log.nopre.opt

../../bin/ptamc csma/abst/csma-bus.des csma/abst/csma-bmax1-s1.des csma/abst/csma-bmax1-s2.des '*:finish:finish' 'true' -min -nopre -opt -refine=all >! logs/forwards.csma1.abst..log.nopre.opt.all
../../bin/ptamc csma/abst/csma-bus.des csma/abst/csma-bmax1-s1.des csma/abst/csma-bmax1-s2.des csma/abst/deadline1000.des '*:*:*:before' 'true' -min -nopre -opt -refine=all   >! logs/forwards.csma1.abst..1000.log.nopre.opt.all
../../bin/ptamc csma/abst/csma-bus.des csma/abst/csma-bmax1-s1.des csma/abst/csma-bmax1-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -nopre -opt -refine=all   >! logs/forwards.csma1.abst..2000.log.nopre.opt.all
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline3000.des '*:*:*:before' 'true' -min -nopre -opt -refine=all   >! logs/forwards.csma1.abst..3000.log.nopre.opt.all

#../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline4000.des '*:*:*:before' 'true' -min -nopre -opt -refine=all   >! logs/forwards.csma1.abst..4000.log.nopre.opt.all

