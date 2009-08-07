# csma example from fmics paper

# eventually both stations send their packets
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des '*:done:done' 'true' -min
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des '*:done:done' 'true' -min
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des '*:done:done' 'true' -min
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des '*:done:done' 'true' -min
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des '*:done:done' 'true' -min

# deadline property
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/deadline2000.des '*:*:*:before' 'true' -min -refine

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/deadline2000.des '*:*:*:after' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/deadline2000.des '*:*:*:after' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/deadline2000.des '*:*:*:after' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/deadline2000.des '*:*:*:after' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/deadline2000.des '*:*:*:after' 'true' -max -refine

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c1' 'true' -max
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c1' 'true' -max
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c1' 'true' -max
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c1' 'true' -max
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c1' 'true' -max

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c2' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c2' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c2' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c2' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c2' 'true' -max -refine

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c3' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c3' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c3' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c3' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c3' 'true' -max -refine

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c4' 'true' -max -refine

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c5' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c5' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c5' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c5' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c5' 'true' -max -refine

../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax1-s1.des csma/des/csmacd-bmax1-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax2-s1.des csma/des/csmacd-bmax2-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax3-s1.des csma/des/csmacd-bmax3-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax4-s1.des csma/des/csmacd-bmax4-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -refine
../../bin/ptamc csma/des/csmacd-bus.des csma/des/csmacd-bmax5-s1.des csma/des/csmacd-bmax5-s2.des csma/des/csmacd-collision-counter.des '*:*:*:c8' 'true' -max -refine

################################################################################

# abstract version
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des '*:finish:finish' 'z<=2000' -max

# deadline property
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1200.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1400.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1600.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline1800.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax1-s1.des csma/abst/csmacd-bmax1-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -refine



../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax2-s1.des csma/abst/csmacd-bmax2-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax3-s1.des csma/abst/csmacd-bmax3-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax4-s1.des csma/abst/csmacd-bmax4-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -refine
../../bin/ptamc csma/abst/csmacd-bus.des csma/abst/csmacd-bmax5-s1.des csma/abst/csmacd-bmax5-s2.des csma/abst/deadline2000.des '*:*:*:before' 'true' -min -refine

