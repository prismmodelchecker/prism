# probability protocol terminates correctly with no errors
../../bin/ptamc repudiation/originator.des repudiation/recipient.des 'done:*' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des 'done:*' 'true' -min

../../bin/ptamc repudiation/originator.des repudiation/recipient.des 'done:*' 'true' -max
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des 'done:*' 'true' -max -refine

# probability protocol terminates early but no information passed
../../bin/ptamc repudiation/originator.des repudiation/recipient.des 'done_early:*' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des 'done_early:*' 'true' -min -refine
../../bin/ptamc repudiation/originator.des repudiation/recipient.des 'done_early:*' 'true' -max 
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des 'done_early:*' 'true' -max -refine

# probability protocol terminates and recipient has gained information
../../bin/ptamc repudiation/originator.des repudiation/recipient.des 'done_error:*' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des 'done_error:*' 'true' -min -refine
../../bin/ptamc repudiation/originator.des repudiation/recipient.des 'done_error:*' 'true' -max
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des 'done_error:*' 'true' -max -refine

# time bounded versions (used for timeliness properties)
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline10.des '*:*:before' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline20.des '*:*:before' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline100.des '*:*:before' 'true' -min

../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline10.des '*:*:before' 'true' -max -refine
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline20.des '*:*:before' 'true' -max -refine
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline100.des '*:*:before' 'true' -max -refine

../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des repudiation/deadline10.des '*:*:before' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des repudiation/deadline20.des '*:*:before' 'true' -min
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des repudiation/deadline100.des '*:*:before' 'true' -min

# also min/max probability finish correctly and within a deadline (lots of combinations...)
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline10.des 'done:*:before' 'true' -max -refine
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline20.des 'done:*:before' 'true' -max -refine
../../bin/ptamc repudiation/originator.des repudiation/recipient.des repudiation/deadline100.des 'done:*:before' 'true' -max -refine

../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des repudiation/deadline10.des 'done_error:*:before' 'true' -max -refine
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des repudiation/deadline20.des 'done_error:*:before' 'true' -max -refine
../../bin/ptamc repudiation/originator.des repudiation/malicous_recipient.des repudiation/deadline100.des 'done_error:*:before' 'true' -max -refine

